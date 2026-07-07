package com.wo.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.api.client.UserClient;
import com.wo.api.dto.user.UserInfo;
import com.wo.api.dto.workorder.CommentCreateDTO;
import com.wo.api.dto.workorder.CommentVO;
import com.wo.common.enums.ErrorCode;
import com.wo.common.exception.BizException;
import com.wo.common.result.R;
import com.wo.common.util.SecurityUtil;
import com.wo.workorder.entity.WoComment;
import com.wo.workorder.entity.WoFlowRecord;
import com.wo.workorder.entity.WoWorkOrder;
import com.wo.workorder.mapper.CommentMapper;
import com.wo.workorder.mapper.FlowRecordMapper;
import com.wo.workorder.mapper.WorkOrderMapper;
import com.wo.workorder.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final WorkOrderMapper workOrderMapper;
    private final FlowRecordMapper flowRecordMapper;
    private final UserClient userClient;

    @Override
    public List<CommentVO> getComments(Long workOrderId) {
        WoWorkOrder workOrder = requireReadableWorkOrder(workOrderId);
        LambdaQueryWrapper<WoComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WoComment::getWorkOrderId, workOrderId)
                .orderByAsc(WoComment::getCreatedAt);
        if (isSubmitterOnly(workOrder)) {
            wrapper.eq(WoComment::getIsInternal, 0);
        }

        List<WoComment> comments = commentMapper.selectList(wrapper);

        return comments.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public CommentVO addComment(Long workOrderId, CommentCreateDTO dto, Long userId, boolean isAiGenerated) {
        WoWorkOrder workOrder = requireReadableWorkOrder(workOrderId);
        if (SecurityUtil.hasRequestContext() && !userId.equals(SecurityUtil.getCurrentUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "评论人与登录用户不一致");
        }
        boolean internal = Boolean.TRUE.equals(dto.getIsInternal());
        if (internal && !canWriteInternalComment()) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权添加内部备注");
        }
        WoComment comment = new WoComment();
        comment.setWorkOrderId(workOrderId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setIsInternal(internal ? 1 : 0);
        comment.setIsAiGenerated(isAiGenerated ? 1 : 0);

        commentMapper.insert(comment);
        saveFlowRecord(workOrder, userId, dto.getContent(), isAiGenerated);
        log.info("评论添加成功, workOrderId={}, userId={}, isAi={}", workOrderId, userId, isAiGenerated);
        return convertToVO(comment);
    }

    private CommentVO convertToVO(WoComment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);

        // 转换Boolean字段
        vo.setIsInternal(comment.getIsInternal() != null && comment.getIsInternal() == 1);
        vo.setIsAiGenerated(comment.getIsAiGenerated() != null && comment.getIsAiGenerated() == 1);

        // 获取用户名
        if (comment.getUserId() != null) {
            try {
                R<UserInfo> resp = userClient.getUserInfo(comment.getUserId());
                if (resp != null && resp.getData() != null) {
                    vo.setUserName(resp.getData().getRealName());
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败, userId={}", comment.getUserId(), e);
            }
        }

        return vo;
    }

    private WoWorkOrder requireReadableWorkOrder(Long workOrderId) {
        WoWorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WO_NOT_FOUND);
        }
        if (!SecurityUtil.hasRequestContext()) {
            return workOrder;
        }
        Long userId = SecurityUtil.getCurrentUserId();
        String role = SecurityUtil.getCurrentRole();
        if (userId == null || !StringUtils.hasText(role)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if ("ADMIN".equals(role)
                || userId.equals(workOrder.getCreatorId())
                || userId.equals(workOrder.getAssigneeId())) {
            return workOrder;
        }
        String department = currentUserDepartment(userId);
        if (StringUtils.hasText(department)
                && department.equals(workOrder.getDepartment())
                && ("AGENT".equals(role) || "MANAGER".equals(role))) {
            return workOrder;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权访问该工单评论");
    }

    private boolean isSubmitterOnly(WoWorkOrder workOrder) {
        Long userId = SecurityUtil.getCurrentUserId();
        String role = SecurityUtil.getCurrentRole();
        return "USER".equals(role) && userId != null && userId.equals(workOrder.getCreatorId());
    }

    private String currentUserDepartment(Long userId) {
        try {
            R<UserInfo> resp = userClient.getUserInfo(userId);
            if (resp != null && resp.getData() != null) {
                return resp.getData().getDepartment();
            }
        } catch (Exception e) {
            log.warn("获取当前用户部门失败, userId={}", userId, e);
        }
        return null;
    }

    private boolean canWriteInternalComment() {
        String role = SecurityUtil.getCurrentRole();
        return "ADMIN".equals(role) || "MANAGER".equals(role) || "AGENT".equals(role);
    }

    private void saveFlowRecord(WoWorkOrder workOrder, Long userId, String content, boolean isAiGenerated) {
        WoFlowRecord record = new WoFlowRecord();
        record.setWorkOrderId(workOrder.getId());
        record.setAction(isAiGenerated ? "AI_COMMENT" : "COMMENT");
        record.setFromStatus(workOrder.getStatus());
        record.setToStatus(workOrder.getStatus());
        record.setOperatorId(userId);
        record.setComment(content);
        record.setIsSystem(isAiGenerated ? 1 : 0);
        flowRecordMapper.insert(record);
    }
}
