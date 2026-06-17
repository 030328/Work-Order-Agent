package com.wo.agent.tool;

import com.wo.agent.feign.WorkOrderToolFeignBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单管理工具集
 * 提供工单的 CRUD 操作，供 AI Agent 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderTool {

    private final WorkOrderToolFeignBridge workOrderBridge;

    /**
     * 创建工单
     */
    public String createWorkOrder(String title, String description, String priority, String category) {
        log.info("Tool: createWorkOrder - title: {}, priority: {}, category: {}", title, priority, category);
        return workOrderBridge.createWorkOrder(title, description, priority, category);
    }

    /**
     * 搜索工单
     */
    public String searchWorkOrders(String keyword, String status, String priority) {
        log.info("Tool: searchWorkOrders - keyword: {}, status: {}, priority: {}", keyword, status, priority);
        return workOrderBridge.searchWorkOrders(keyword, status, priority);
    }

    /**
     * 获取工单详情
     */
    public String getWorkOrderDetail(Long workOrderId) {
        log.info("Tool: getWorkOrderDetail - workOrderId: {}", workOrderId);
        return workOrderBridge.getWorkOrderDetail(workOrderId);
    }

    /**
     * 更新工单状态
     */
    public String updateWorkOrderStatus(Long workOrderId, String targetStatus, String comment) {
        log.info("Tool: updateWorkOrderStatus - workOrderId: {}, targetStatus: {}", workOrderId, targetStatus);
        return workOrderBridge.updateWorkOrderStatus(workOrderId, targetStatus, comment);
    }

    /**
     * 指派工单
     */
    public String assignWorkOrder(Long workOrderId, Long assigneeId, String reason) {
        log.info("Tool: assignWorkOrder - workOrderId: {}, assigneeId: {}", workOrderId, assigneeId);
        return workOrderBridge.assignWorkOrder(workOrderId, assigneeId, reason);
    }

    /**
     * 添加评论
     */
    public String addComment(Long workOrderId, String content, boolean isInternal) {
        log.info("Tool: addComment - workOrderId: {}, isInternal: {}", workOrderId, isInternal);
        return workOrderBridge.addComment(workOrderId, content, isInternal);
    }
}
