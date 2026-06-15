package com.wo.agent.tool;

import com.wo.agent.feign.WorkOrderToolFeignBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 工单管理工具集
 * 提供工单的 CRUD 操作，供 AI Agent 通过 Tool Calling 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderTool {

    private final WorkOrderToolFeignBridge workOrderBridge;

    /**
     * 创建工单
     */
    @Tool(description = "创建新的工单。当用户报告问题、请求服务或需要记录工作时调用此工具。" +
            "请从用户描述中提取标题、描述、优先级和分类。")
    public String createWorkOrder(
            @ToolParam(description = "工单标题，简明扼要地描述问题") String title,
            @ToolParam(description = "工单详细描述，包含问题现象、影响范围等") String description,
            @ToolParam(description = "优先级：low-低, medium-中, high-高, urgent-紧急") String priority,
            @ToolParam(description = "分类：hardware-硬件, software-软件, network-网络, access-权限, other-其他") String category) {
        log.info("Tool: createWorkOrder - title: {}, priority: {}, category: {}", title, priority, category);
        return workOrderBridge.createWorkOrder(title, description, priority, category);
    }

    /**
     * 搜索工单
     */
    @Tool(description = "搜索工单列表。支持按关键词、状态、优先级筛选。" +
            "当用户询问工单情况、查找特定工单时调用。")
    public String searchWorkOrders(
            @ToolParam(description = "搜索关键词，匹配标题和描述") String keyword,
            @ToolParam(description = "工单状态：open-待处理, in_progress-处理中, resolved-已解决, closed-已关闭") String status,
            @ToolParam(description = "优先级：low, medium, high, urgent") String priority) {
        log.info("Tool: searchWorkOrders - keyword: {}, status: {}, priority: {}", keyword, status, priority);
        return workOrderBridge.searchWorkOrders(keyword, status, priority);
    }

    /**
     * 获取工单详情
     */
    @Tool(description = "获取工单的详细信息，包括状态、指派人、评论等。" +
            "当用户询问特定工单详情时调用。")
    public String getWorkOrderDetail(
            @ToolParam(description = "工单ID") Long workOrderId) {
        log.info("Tool: getWorkOrderDetail - workOrderId: {}", workOrderId);
        return workOrderBridge.getWorkOrderDetail(workOrderId);
    }

    /**
     * 更新工单状态
     */
    @Tool(description = "更新工单状态。支持状态流转：open -> in_progress -> resolved -> closed。" +
            "当用户要求改变工单状态时调用。")
    public String updateWorkOrderStatus(
            @ToolParam(description = "工单ID") Long workOrderId,
            @ToolParam(description = "目标状态：open, in_progress, resolved, closed") String targetStatus,
            @ToolParam(description = "状态变更说明") String comment) {
        log.info("Tool: updateWorkOrderStatus - workOrderId: {}, targetStatus: {}", workOrderId, targetStatus);
        return workOrderBridge.updateWorkOrderStatus(workOrderId, targetStatus, comment);
    }

    /**
     * 指派工单
     */
    @Tool(description = "将工单指派给指定处理人。当用户要求将工单分配给某人时调用。")
    public String assignWorkOrder(
            @ToolParam(description = "工单ID") Long workOrderId,
            @ToolParam(description = "处理人用户ID") Long assigneeId,
            @ToolParam(description = "指派原因说明") String reason) {
        log.info("Tool: assignWorkOrder - workOrderId: {}, assigneeId: {}", workOrderId, assigneeId);
        return workOrderBridge.assignWorkOrder(workOrderId, assigneeId, reason);
    }

    /**
     * 添加评论
     */
    @Tool(description = "为工单添加评论或备注。支持公开评论和内部备注。" +
            "当用户要求为工单添加说明时调用。")
    public String addComment(
            @ToolParam(description = "工单ID") Long workOrderId,
            @ToolParam(description = "评论内容") String content,
            @ToolParam(description = "是否为内部备注（仅内部人员可见）") boolean isInternal) {
        log.info("Tool: addComment - workOrderId: {}, isInternal: {}", workOrderId, isInternal);
        return workOrderBridge.addComment(workOrderId, content, isInternal);
    }
}
