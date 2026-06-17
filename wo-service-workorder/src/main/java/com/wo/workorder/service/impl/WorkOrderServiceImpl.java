package com.wo.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wo.api.client.AiAgentClient;
import com.wo.api.client.UserClient;
import com.wo.api.dto.ai.WorkOrderAnalysisRequest;
import com.wo.api.dto.ai.WorkOrderAnalysisResult;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.api.dto.workorder.WorkOrderCreateDTO;
import com.wo.api.dto.workorder.WorkOrderQueryDTO;
import com.wo.api.dto.workorder.WorkOrderVO;
import com.wo.common.enums.ErrorCode;
import com.wo.common.enums.WorkOrderStatus;
import com.wo.common.exception.BizException;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import com.wo.workorder.entity.WoFlowRecord;
import com.wo.workorder.entity.WoWorkOrder;
import com.wo.workorder.mapper.FlowRecordMapper;
import com.wo.workorder.mapper.WorkOrderMapper;
import com.wo.workorder.service.WorkOrderEventPublisher;
import com.wo.workorder.service.WorkOrderSearchService;
import com.wo.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final FlowRecordMapper flowRecordMapper;
    private final UserClient userClient;
    private final AiAgentClient aiAgentClient;
    private final WorkOrderSearchService workOrderSearchService;
    private final WorkOrderEventPublisher workOrderEventPublisher;

    private static final DateTimeFormatter ORDER_NO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public WorkOrderVO getWorkOrder(Long id) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }
        return convertToVO(workOrder);
    }

    @Override
    public PageResult<WorkOrderBriefVO> queryWorkOrders(WorkOrderQueryDTO query) {
        Page<WoWorkOrder> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<WoWorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getKeyword()), WoWorkOrder::getTitle, query.getKeyword())
                .eq(StringUtils.hasText(query.getStatus()), WoWorkOrder::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getPriority()), WoWorkOrder::getPriority, query.getPriority())
                .eq(StringUtils.hasText(query.getCategory()), WoWorkOrder::getCategory, query.getCategory())
                .eq(query.getAssigneeId() != null, WoWorkOrder::getAssigneeId, query.getAssigneeId())
                .eq(query.getCreatorId() != null, WoWorkOrder::getCreatorId, query.getCreatorId())
                .ge(query.getStartDate() != null, WoWorkOrder::getCreatedAt, query.getStartDate())
                .le(query.getEndDate() != null, WoWorkOrder::getCreatedAt, query.getEndDate())
                .orderByDesc(WoWorkOrder::getCreatedAt);

        Page<WoWorkOrder> result = workOrderMapper.selectPage(page, wrapper);

        List<WorkOrderBriefVO> voList = result.getRecords().stream()
                .map(this::convertToBriefVO)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderVO createWorkOrder(WorkOrderCreateDTO dto, Long creatorId) {
        WoWorkOrder workOrder = new WoWorkOrder();
        BeanUtils.copyProperties(dto, workOrder);

        workOrder.setOrderNo(generateOrderNo());
        workOrder.setStatus(WorkOrderStatus.OPEN.getCode());
        workOrder.setCreatorId(creatorId);

        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            workOrder.setTags(String.join(",", dto.getTags()));
        }

        workOrderMapper.insert(workOrder);
        log.info("工单创建成功, id={}, orderNo={}", workOrder.getId(), workOrder.getOrderNo());

        // 保存流程记录
        saveFlowRecord(workOrder.getId(), "CREATE", null, WorkOrderStatus.OPEN.getCode(),
                creatorId, "创建工单", 1);

        // 发布创建事件
        workOrderEventPublisher.publishStatusChangeEvent(workOrder.getId(), null, WorkOrderStatus.OPEN.getCode());

        // 索引到ES
        workOrderSearchService.indexWorkOrder(workOrder);

        // 异步调用AI分析
        try {
            WorkOrderAnalysisRequest analysisRequest = WorkOrderAnalysisRequest.builder()
                    .workOrderId(workOrder.getId())
                    .title(workOrder.getTitle())
                    .description(workOrder.getDescription())
                    .category(workOrder.getCategory())
                    .priority(workOrder.getPriority())
                    .build();

            R<WorkOrderAnalysisResult> analysisResult = aiAgentClient.analyzeWorkOrder(analysisRequest);
            if (analysisResult != null && analysisResult.getData() != null) {
                WorkOrderAnalysisResult aiResult = analysisResult.getData();
                workOrder.setAiSummary(aiResult.getSummary());
                workOrder.setAiSentiment(aiResult.getSentiment());
                workOrder.setAiCategorySuggestion(aiResult.getSuggestedCategory());
                workOrder.setAiSuggestedSolution(aiResult.getSuggestedSolution());
                workOrderMapper.updateById(workOrder);
                log.info("AI分析完成, id={}", workOrder.getId());
            }
        } catch (Exception e) {
            log.warn("AI分析失败, 工单正常创建, id={}", workOrder.getId(), e);
        }

        return convertToVO(workOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status, Long operatorId, String comment) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }

        String fromStatus = workOrder.getStatus();

        // 验证状态转换合法性
        validateStatusTransition(fromStatus, status);

        workOrder.setStatus(status);

        // 设置解决/关闭时间
        if (WorkOrderStatus.RESOLVED.getCode().equals(status)) {
            workOrder.setResolvedAt(LocalDateTime.now());
        } else if (WorkOrderStatus.CLOSED.getCode().equals(status)) {
            workOrder.setClosedAt(LocalDateTime.now());
        }

        workOrderMapper.updateById(workOrder);
        log.info("工单状态更新, id={}, from={}, to={}", id, fromStatus, status);

        // 保存流程记录
        saveFlowRecord(id, "STATUS_CHANGE", fromStatus, status, operatorId, comment, 0);

        // 发布事件
        workOrderEventPublisher.publishStatusChangeEvent(id, fromStatus, status);

        if (WorkOrderStatus.CLOSED.getCode().equals(status)) {
            workOrderEventPublisher.publishCloseEvent(id);
        }

        // 更新ES索引
        workOrderSearchService.indexWorkOrder(workOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignWorkOrder(Long id, Long assigneeId, Long operatorId, String reason) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }

        String fromStatus = workOrder.getStatus();
        workOrder.setAssigneeId(assigneeId);

        // 如果是待分配状态，自动变更为处理中
        if (WorkOrderStatus.OPEN.getCode().equals(fromStatus)) {
            workOrder.setStatus(WorkOrderStatus.IN_PROGRESS.getCode());
        }

        workOrderMapper.updateById(workOrder);
        log.info("工单分配成功, id={}, assigneeId={}", id, assigneeId);

        // 保存流程记录
        String toStatus = workOrder.getStatus();
        saveFlowRecord(id, "ASSIGN", fromStatus, toStatus, operatorId, reason, 0);

        // 发布事件
        if (!fromStatus.equals(toStatus)) {
            workOrderEventPublisher.publishStatusChangeEvent(id, fromStatus, toStatus);
        }

        // 更新ES索引
        workOrderSearchService.indexWorkOrder(workOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAiSummary(Long id, String summary, String sentiment) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }

        workOrder.setAiSummary(summary);
        workOrder.setAiSentiment(sentiment);
        workOrderMapper.updateById(workOrder);
        log.info("AI摘要添加成功, id={}", id);
    }

    /**
     * 生成工单编号: WO-yyyyMMdd-HHmmss-XXX
     */
    private String generateOrderNo() {
        String dateStr = LocalDate.now().format(ORDER_NO_DATE_FMT);
        String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"));
        int random = (int) (Math.random() * 1000);
        return String.format("WO-%s-%s-%03d", dateStr, timeStr, random);
    }

    /**
     * 验证状态转换合法性
     */
    private void validateStatusTransition(String from, String to) {
        boolean valid = switch (from) {
            case "DRAFT" -> "OPEN".equals(to);
            case "OPEN" -> "IN_PROGRESS".equals(to) || "CLOSED".equals(to);
            case "IN_PROGRESS" -> "PENDING_REVIEW".equals(to) || "RESOLVED".equals(to) || "OPEN".equals(to);
            case "PENDING_REVIEW" -> "RESOLVED".equals(to) || "IN_PROGRESS".equals(to) || "REJECTED".equals(to);
            case "RESOLVED" -> "CLOSED".equals(to) || "IN_PROGRESS".equals(to);
            case "REJECTED" -> "IN_PROGRESS".equals(to) || "CLOSED".equals(to);
            case "CLOSED" -> false;
            default -> false;
        };

        if (!valid) {
            throw new BizException(ErrorCode.WF_TRANSITION_INVALID,
                    String.format("不允许从 %s 转换到 %s", from, to));
        }
    }

    /**
     * 保存流程记录
     */
    private void saveFlowRecord(Long workOrderId, String action, String fromStatus,
                                String toStatus, Long operatorId, String comment, int isSystem) {
        WoFlowRecord record = new WoFlowRecord();
        record.setWorkOrderId(workOrderId);
        record.setAction(action);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setOperatorId(operatorId);
        record.setComment(comment);
        record.setIsSystem(isSystem);
        flowRecordMapper.insert(record);
    }

    /**
     * 转换为VO，填充用户名
     */
    private WorkOrderVO convertToVO(WoWorkOrder workOrder) {
        WorkOrderVO vo = new WorkOrderVO();
        BeanUtils.copyProperties(workOrder, vo);

        // tags转换
        if (StringUtils.hasText(workOrder.getTags())) {
            vo.setTags(List.of(workOrder.getTags().split(",")));
        }

        // 通过Feign获取用户信息
        fillUserInfo(vo, workOrder.getCreatorId(), workOrder.getAssigneeId());

        return vo;
    }

    /**
     * 转换为简要VO
     */
    private WorkOrderBriefVO convertToBriefVO(WoWorkOrder workOrder) {
        WorkOrderBriefVO vo = new WorkOrderBriefVO();
        BeanUtils.copyProperties(workOrder, vo);

        // 填充处理人名称
        if (workOrder.getAssigneeId() != null) {
            try {
                R<UserInfo> resp = userClient.getUserInfo(workOrder.getAssigneeId());
                if (resp != null && resp.getData() != null) {
                    vo.setAssigneeName(resp.getData().getRealName());
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败, userId={}", workOrder.getAssigneeId(), e);
            }
        }

        return vo;
    }

    /**
     * 填充创建人和处理人信息
     */
    private void fillUserInfo(WorkOrderVO vo, Long creatorId, Long assigneeId) {
        if (creatorId != null) {
            try {
                R<UserInfo> resp = userClient.getUserInfo(creatorId);
                if (resp != null && resp.getData() != null) {
                    vo.setCreatorName(resp.getData().getRealName());
                }
            } catch (Exception e) {
                log.warn("获取创建人信息失败, userId={}", creatorId, e);
            }
        }
        if (assigneeId != null) {
            try {
                R<UserInfo> resp = userClient.getUserInfo(assigneeId);
                if (resp != null && resp.getData() != null) {
                    vo.setAssigneeName(resp.getData().getRealName());
                }
            } catch (Exception e) {
                log.warn("获取处理人信息失败, userId={}", assigneeId, e);
            }
        }
    }
}
