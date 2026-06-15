package com.wo.workorder.service;

import com.wo.api.dto.workorder.WorkOrderBriefVO;
import com.wo.api.dto.workorder.WorkOrderCreateDTO;
import com.wo.api.dto.workorder.WorkOrderQueryDTO;
import com.wo.api.dto.workorder.WorkOrderVO;
import com.wo.common.result.PageResult;

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
}
