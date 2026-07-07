package com.wo.workorder.service;

import com.wo.api.dto.workorder.FlowRecordVO;
import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.api.dto.workorder.WorkOrderCreateDTO;
import com.wo.api.dto.workorder.WorkOrderQueryDTO;
import com.wo.api.dto.workorder.WorkOrderVO;
import com.wo.common.result.PageResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface WorkOrderService {

    /**
     * 获取工单详情
     */
    WorkOrderVO getWorkOrder(Long id);

    /**
     * 分页查询工单
     */
    PageResult<WorkOrderBriefVO> queryWorkOrders(WorkOrderQueryDTO query);

    /**
     * 按状态统计工单数量
     */
    Map<String, Long> countWorkOrdersByStatus(WorkOrderQueryDTO query);

    /**
     * 创建工单
     */
    WorkOrderVO createWorkOrder(WorkOrderCreateDTO dto, Long creatorId);

    /**
     * 更新工单状态
     */
    void updateStatus(Long id, String status, Long operatorId, String comment);

    /**
     * 分配工单
     */
    void assignWorkOrder(Long id, Long assigneeId, Long operatorId, String reason);

    /**
     * 添加AI摘要
     */
    void addAiSummary(Long id, String summary, String sentiment);

    /**
     * 用户确认工单完成
     */
    void confirmWorkOrder(Long id, Long userId);

    /**
     * 驳回人工处理结果，退回处理中
     */
    void rejectResolution(Long id, Long userId, String reason);

    /**
     * AI 重新生成解决方案
     */
    WorkOrderVO regenerateSolution(Long id, Long userId);

    /**
     * 转人工处理
     */
    void escalateWorkOrder(Long id, Long userId);

    /**
     * 认领工单
     */
    void claimWorkOrder(Long id, Long userId);

    /**
     * 获取部门待处理工单
     */
    PageResult<WorkOrderBriefVO> getDepartmentWorkOrders(String department, int page, int size);

    /**
     * 查询工单流转记录
     */
    List<FlowRecordVO> listFlowRecords(Long id);

    /**
     * 更新 SLA 截止时间
     */
    void updateSlaDeadline(Long id, LocalDateTime deadline);

    /**
     * 查询已经超过 SLA 且仍需处理的工单 ID
     */
    List<Long> listSlaBreachedWorkOrderIds(LocalDateTime deadlineBefore);

    /**
     * 标记 SLA 超时并转入人工升级队列
     */
    void markSlaBreached(Long id);
}
