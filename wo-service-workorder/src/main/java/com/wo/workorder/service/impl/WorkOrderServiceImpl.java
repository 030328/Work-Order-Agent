package com.wo.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wo.api.client.AiAgentClient;
import com.wo.api.client.UserClient;
import com.wo.api.client.WorkflowClient;
import com.wo.api.dto.ai.WorkOrderAnalysisRequest;
import com.wo.api.dto.ai.WorkOrderAnalysisResult;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.api.dto.workorder.WorkOrderCreateDTO;
import com.wo.api.dto.workorder.WorkOrderQueryDTO;
import com.wo.api.dto.workorder.WorkOrderVO;
import com.wo.api.dto.workflow.TransitionRequest;
import com.wo.api.dto.workflow.TransitionResult;
import com.wo.common.enums.ErrorCode;
import com.wo.common.enums.WorkOrderStatus;
import com.wo.common.exception.BizException;
import com.wo.common.result.PageResult;
import com.wo.common.result.R;
import com.wo.common.util.SecurityUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final FlowRecordMapper flowRecordMapper;
    private final UserClient userClient;
    private final AiAgentClient aiAgentClient;
    private final WorkflowClient workflowClient;
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
    public Map<String, Long> countWorkOrdersByStatus(WorkOrderQueryDTO query) {
        QueryWrapper<WoWorkOrder> wrapper = new QueryWrapper<>();
        wrapper.select("status", "COUNT(*) AS count")
                .like(StringUtils.hasText(query.getKeyword()), "title", query.getKeyword())
                .eq(StringUtils.hasText(query.getPriority()), "priority", query.getPriority())
                .eq(StringUtils.hasText(query.getCategory()), "category", query.getCategory())
                .eq(query.getAssigneeId() != null, "assignee_id", query.getAssigneeId())
                .eq(query.getCreatorId() != null, "creator_id", query.getCreatorId())
                .ge(query.getStartDate() != null, "created_at", query.getStartDate())
                .le(query.getEndDate() != null, "created_at", query.getEndDate())
                .groupBy("status");

        Map<String, Long> counts = new HashMap<>();
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            counts.put(status.getCode(), 0L);
        }

        workOrderMapper.selectMaps(wrapper).forEach(row -> {
            Object status = row.get("status");
            Object count = row.get("count");
            if (status != null && count instanceof Number number) {
                counts.put(String.valueOf(status), number.longValue());
            }
        });
        return counts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderVO createWorkOrder(WorkOrderCreateDTO dto, Long creatorId) {
        WoWorkOrder workOrder = new WoWorkOrder();
        BeanUtils.copyProperties(dto, workOrder);

        workOrder.setOrderNo(generateOrderNo());
        workOrder.setStatus(WorkOrderStatus.OPEN.getCode());
        workOrder.setCreatorId(creatorId);
        applySlaDeadline(workOrder);

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

        // 调用AI分析（包含 ES 相似工单检索）
        try {
            // 1. 从 ES 检索相似历史工单
            List<WorkOrderAnalysisRequest.SimilarWorkOrder> similarOrders = findSimilarWorkOrders(workOrder);

            // 2. 构建请求
            WorkOrderAnalysisRequest analysisRequest = WorkOrderAnalysisRequest.builder()
                    .workOrderId(workOrder.getId())
                    .title(workOrder.getTitle())
                    .description(workOrder.getDescription())
                    .category(workOrder.getCategory())
                    .priority(workOrder.getPriority())
                    .similarWorkOrders(similarOrders)
                    .build();

            // 3. 调用 AI Agent
            R<WorkOrderAnalysisResult> analysisResult = aiAgentClient.analyzeWorkOrder(analysisRequest);
            if (analysisResult != null && analysisResult.getData() != null) {
                WorkOrderAnalysisResult aiResult = analysisResult.getData();
                workOrder.setAiSummary(aiResult.getSummary());
                workOrder.setAiSentiment(aiResult.getSentiment());
                workOrder.setAiCategorySuggestion(aiResult.getSuggestedCategory());
                workOrder.setAiSuggestedSolution(aiResult.getSuggestedSolution());
                workOrder.setStatus(WorkOrderStatus.AI_SOLVED.getCode());
                workOrderMapper.updateById(workOrder);
                saveFlowRecord(workOrder.getId(), "AI_SOLVED", WorkOrderStatus.OPEN.getCode(),
                        WorkOrderStatus.AI_SOLVED.getCode(), 0L, "AI已给出处理建议", 1);
                workOrderEventPublisher.publishStatusChangeEvent(workOrder.getId(),
                        WorkOrderStatus.OPEN.getCode(), WorkOrderStatus.AI_SOLVED.getCode());
                workOrderSearchService.indexWorkOrder(workOrder);
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

        validateStatusTransition(workOrder.getId(), fromStatus, status, operatorId, SecurityUtil.getCurrentRole(), comment);

        workOrder.setStatus(status);

        // 设置解决/关闭时间
        if (WorkOrderStatus.RESOLVED.getCode().equals(status)) {
            workOrder.setResolvedAt(LocalDateTime.now());
            if (StringUtils.hasText(comment)) {
                workOrder.setResolution(comment);
            }
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
            validateStatusTransition(id, fromStatus, WorkOrderStatus.IN_PROGRESS.getCode(),
                    operatorId, SecurityUtil.getCurrentRole(), reason);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmWorkOrder(Long id, Long userId) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }

        ensureCreator(workOrder, userId, "确认工单完成");

        String fromStatus = workOrder.getStatus();
        if (!WorkOrderStatus.AI_SOLVED.getCode().equals(fromStatus)
                && !WorkOrderStatus.RESOLVED.getCode().equals(fromStatus)) {
            throw new BizException(ErrorCode.WO_STATUS_INVALID, "只有AI已处理或人工已解决的工单才能确认完成");
        }

        validateStatusTransition(id, fromStatus, WorkOrderStatus.CLOSED.getCode(),
                userId, SecurityUtil.getCurrentRole(), "用户确认完成");

        workOrder.setStatus(WorkOrderStatus.CLOSED.getCode());
        workOrder.setClosedAt(LocalDateTime.now());
        workOrderMapper.updateById(workOrder);

        saveFlowRecord(id, "CONFIRM", fromStatus, WorkOrderStatus.CLOSED.getCode(), userId, "用户确认完成", 0);
        workOrderEventPublisher.publishStatusChangeEvent(id, fromStatus, WorkOrderStatus.CLOSED.getCode());
        workOrderEventPublisher.publishCloseEvent(id);
        workOrderSearchService.indexWorkOrder(workOrder);

        // 异步：AI 总结经验，写入知识库
        try {
            String summary = String.format("工单：%s\n问题描述：%s\n解决方案：%s",
                    workOrder.getTitle(), workOrder.getDescription(), workOrder.getAiSuggestedSolution());
            // TODO: 调用 AI Agent 索引到 Milvus
            log.info("工单经验已记录, id={}", id);
        } catch (Exception e) {
            log.warn("工单经验记录失败, id={}", id, e);
        }

        log.info("工单确认完成, id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderVO regenerateSolution(Long id, Long userId) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }

        // 重新调用 AI 分析
        try {
            List<WorkOrderAnalysisRequest.SimilarWorkOrder> similarOrders = findSimilarWorkOrders(workOrder);
            WorkOrderAnalysisRequest analysisRequest = WorkOrderAnalysisRequest.builder()
                    .workOrderId(workOrder.getId())
                    .title(workOrder.getTitle())
                    .description(workOrder.getDescription())
                    .category(workOrder.getCategory())
                    .priority(workOrder.getPriority())
                    .similarWorkOrders(similarOrders)
                    .build();

            R<WorkOrderAnalysisResult> analysisResult = aiAgentClient.analyzeWorkOrder(analysisRequest);
            if (analysisResult != null && analysisResult.getData() != null) {
                WorkOrderAnalysisResult aiResult = analysisResult.getData();
                workOrder.setAiSummary(aiResult.getSummary());
                workOrder.setAiSentiment(aiResult.getSentiment());
                workOrder.setAiCategorySuggestion(aiResult.getSuggestedCategory());
                workOrder.setAiSuggestedSolution(aiResult.getSuggestedSolution());
                workOrderMapper.updateById(workOrder);
                log.info("AI重新分析完成, id={}", id);
            }
        } catch (Exception e) {
            log.warn("AI重新分析失败, id={}", id, e);
        }

        saveFlowRecord(id, "REGENERATE", workOrder.getStatus(), workOrder.getStatus(), userId, "AI重新生成方案", 0);
        return convertToVO(workOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void escalateWorkOrder(Long id, Long userId) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }

        ensureCreator(workOrder, userId, "转人工处理");

        String fromStatus = workOrder.getStatus();
        if (!WorkOrderStatus.AI_SOLVED.getCode().equals(fromStatus)) {
            throw new BizException(ErrorCode.WO_STATUS_INVALID, "只有AI已处理的工单才能转人工处理");
        }

        validateStatusTransition(id, fromStatus, WorkOrderStatus.ESCALATED.getCode(),
                userId, SecurityUtil.getCurrentRole(), "AI建议未解决，转人工处理");

        String department = workOrder.getAiCategorySuggestion() != null ?
                mapCategoryToDepartment(workOrder.getAiCategorySuggestion()) : "技术部";

        workOrder.setStatus(WorkOrderStatus.ESCALATED.getCode());
        workOrder.setDepartment(department);
        workOrder.setEscalatedAt(LocalDateTime.now());
        workOrder.setSlaDeadline(LocalDateTime.now().plusHours(2)); // 2小时 SLA
        workOrderMapper.updateById(workOrder);

        saveFlowRecord(id, "ESCALATE", fromStatus, WorkOrderStatus.ESCALATED.getCode(),
                userId, "转人工处理，分配部门：" + department, 0);
        workOrderEventPublisher.publishStatusChangeEvent(id, fromStatus, WorkOrderStatus.ESCALATED.getCode());
        workOrderSearchService.indexWorkOrder(workOrder);

        log.info("工单转人工, id={}, department={}", id, department);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimWorkOrder(Long id, Long userId) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }

        if (!WorkOrderStatus.ESCALATED.getCode().equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.WO_STATUS_INVALID, "只有转人工状态的工单才能认领");
        }

        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS.getCode());
        workOrder.setAssigneeId(userId);
        workOrder.setClaimedAt(LocalDateTime.now());
        workOrderMapper.updateById(workOrder);

        saveFlowRecord(id, "CLAIM", WorkOrderStatus.ESCALATED.getCode(),
                WorkOrderStatus.IN_PROGRESS.getCode(), userId, "认领工单", 0);

        log.info("工单认领成功, id={}, assigneeId={}", id, userId);
    }

    @Override
    public PageResult<WorkOrderBriefVO> getDepartmentWorkOrders(String department, int page, int size) {
        Page<WoWorkOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<WoWorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WoWorkOrder::getDepartment, department)
                .eq(WoWorkOrder::getStatus, WorkOrderStatus.ESCALATED.getCode())
                .orderByDesc(WoWorkOrder::getCreatedAt);

        Page<WoWorkOrder> result = workOrderMapper.selectPage(pageParam, wrapper);
        List<WorkOrderBriefVO> voList = result.getRecords().stream()
                .map(this::convertToBriefVO)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSlaDeadline(Long id, LocalDateTime deadline) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }

        workOrder.setSlaDeadline(deadline);
        workOrderMapper.updateById(workOrder);
        log.info("SLA deadline updated, id={}, deadline={}", id, deadline);
    }

    @Override
    public List<Long> listSlaBreachedWorkOrderIds(LocalDateTime deadlineBefore) {
        LambdaQueryWrapper<WoWorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(WoWorkOrder::getSlaDeadline)
                .lt(WoWorkOrder::getSlaDeadline, deadlineBefore)
                .notIn(WoWorkOrder::getStatus,
                        WorkOrderStatus.RESOLVED.getCode(),
                        WorkOrderStatus.CLOSED.getCode(),
                        WorkOrderStatus.ESCALATED.getCode())
                .select(WoWorkOrder::getId);

        return workOrderMapper.selectList(wrapper).stream()
                .map(WoWorkOrder::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSlaBreached(Long id) {
        WoWorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }

        if (WorkOrderStatus.RESOLVED.getCode().equals(workOrder.getStatus())
                || WorkOrderStatus.CLOSED.getCode().equals(workOrder.getStatus())
                || WorkOrderStatus.ESCALATED.getCode().equals(workOrder.getStatus())) {
            return;
        }

        String fromStatus = workOrder.getStatus();
        workOrder.setStatus(WorkOrderStatus.ESCALATED.getCode());
        workOrder.setDepartment(workOrder.getDepartment() != null ? workOrder.getDepartment() : "技术部");
        workOrder.setEscalatedAt(LocalDateTime.now());
        workOrderMapper.updateById(workOrder);

        saveFlowRecord(id, "SLA_BREACH", fromStatus, WorkOrderStatus.ESCALATED.getCode(),
                0L, "SLA超时，系统自动转人工处理", 1);
        workOrderEventPublisher.publishStatusChangeEvent(id, fromStatus, WorkOrderStatus.ESCALATED.getCode());
        workOrderSearchService.indexWorkOrder(workOrder);
    }

    private String mapCategoryToDepartment(String category) {
        return switch (category) {
            case "BUG", "INCIDENT" -> "技术部";
            case "FEATURE" -> "产品部";
            case "QUESTION" -> "客服部";
            case "MAINTENANCE" -> "运维部";
            default -> "技术部";
        };
    }

    private void ensureCreator(WoWorkOrder workOrder, Long userId, String action) {
        if (userId == null || workOrder.getCreatorId() == null || !workOrder.getCreatorId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有工单创建人才能" + action);
        }
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
    private void validateStatusTransition(Long workOrderId, String from, String to,
                                          Long operatorId, String operatorRole, String comment) {
        TransitionRequest request = new TransitionRequest();
        request.setWorkOrderId(workOrderId);
        request.setDefinitionId(1L);
        request.setFromStatus(from);
        request.setToStatus(to);
        request.setEvent(resolveWorkflowEvent(from, to));
        request.setOperatorId(operatorId != null ? operatorId : 0L);
        request.setOperatorRole(operatorRole != null ? operatorRole : "USER");
        request.setComment(comment);

        R<TransitionResult> result = workflowClient.executeTransition(request);
        if (result == null || result.getCode() != 0 || result.getData() == null
                || !Boolean.TRUE.equals(result.getData().getSuccess())) {
            String message = result != null ? result.getMessage() : "工作流服务无响应";
            throw new BizException(ErrorCode.WF_TRANSITION_INVALID, message);
        }
    }

    private String resolveWorkflowEvent(String from, String to) {
        if ("DRAFT".equals(from) && WorkOrderStatus.OPEN.getCode().equals(to)) {
            return "submit";
        }
        if (WorkOrderStatus.OPEN.getCode().equals(from) && WorkOrderStatus.IN_PROGRESS.getCode().equals(to)) {
            return "start";
        }
        if (WorkOrderStatus.OPEN.getCode().equals(from) && WorkOrderStatus.CLOSED.getCode().equals(to)) {
            return "cancel";
        }
        if (WorkOrderStatus.OPEN.getCode().equals(from) && WorkOrderStatus.ESCALATED.getCode().equals(to)) {
            return "escalate";
        }
        if (WorkOrderStatus.IN_PROGRESS.getCode().equals(from) && WorkOrderStatus.RESOLVED.getCode().equals(to)) {
            return "resolve";
        }
        if (WorkOrderStatus.RESOLVED.getCode().equals(from) && WorkOrderStatus.CLOSED.getCode().equals(to)) {
            return "close";
        }
        if (WorkOrderStatus.ESCALATED.getCode().equals(from) && WorkOrderStatus.IN_PROGRESS.getCode().equals(to)) {
            return "claim";
        }
        if (WorkOrderStatus.AI_SOLVED.getCode().equals(from) && WorkOrderStatus.CLOSED.getCode().equals(to)) {
            return "confirm_ai_solution";
        }
        if (WorkOrderStatus.AI_SOLVED.getCode().equals(from) && WorkOrderStatus.ESCALATED.getCode().equals(to)) {
            return "reject_ai_solution";
        }
        return "manual";
    }

    private void applySlaDeadline(WoWorkOrder workOrder) {
        try {
            R<String> result = workflowClient.calculateSlaDeadline(workOrder.getPriority());
            if (result != null && result.getCode() == 0 && result.getData() != null) {
                workOrder.setSlaDeadline(LocalDateTime.parse(result.getData()));
            } else {
                log.warn("SLA截止时间计算失败, priority={}, message={}",
                        workOrder.getPriority(), result != null ? result.getMessage() : "empty response");
            }
        } catch (Exception e) {
            log.warn("SLA截止时间计算异常, priority={}", workOrder.getPriority(), e);
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
     * 从 ES 检索相似历史工单（Top 5）
     */
    private List<WorkOrderAnalysisRequest.SimilarWorkOrder> findSimilarWorkOrders(WoWorkOrder workOrder) {
        try {
            String keyword = workOrder.getTitle();
            PageResult<WorkOrderBriefVO> searchResult = workOrderSearchService.search(keyword, 1, 5);

            if (searchResult.getRecords() == null || searchResult.getRecords().isEmpty()) {
                return List.of();
            }

            return searchResult.getRecords().stream()
                    .filter(vo -> !vo.getId().equals(workOrder.getId())) // 排除自己
                    .map(vo -> {
                        // 获取完整工单信息（包含 resolution）
                        WoWorkOrder wo = workOrderMapper.selectById(vo.getId());
                        return WorkOrderAnalysisRequest.SimilarWorkOrder.builder()
                                .id(vo.getId())
                                .orderNo(vo.getOrderNo())
                                .title(vo.getTitle())
                                .description(wo != null ? wo.getDescription() : "")
                                .resolution(wo != null ? wo.getResolution() : "")
                                .status(vo.getStatus())
                                .build();
                    })
                    .limit(5)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("ES检索相似工单失败", e);
            return List.of();
        }
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
                } else {
                    vo.setAssigneeName("用户#" + workOrder.getAssigneeId());
                }
            } catch (Exception e) {
                vo.setAssigneeName("用户#" + workOrder.getAssigneeId());
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
                } else {
                    vo.setAssigneeName("用户#" + assigneeId);
                }
            } catch (Exception e) {
                vo.setAssigneeName("用户#" + assigneeId);
                log.warn("获取处理人信息失败, userId={}", assigneeId, e);
            }
        }
    }
}
