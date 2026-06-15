package com.wo.agent.tool;

import com.wo.agent.feign.WorkOrderToolFeignBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 用户查询工具
 * 提供用户和处理人查询能力，供 AI Agent 通过 Tool Calling 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTool {

    private final WorkOrderToolFeignBridge workOrderBridge;

    /**
     * 查询可用处理人
     * 根据角色和部门筛选可用的工单处理人
     */
    @Tool(description = "查询可用的工单处理人列表。当需要指派工单或了解团队成员时调用。" +
            "可以按角色和部门筛选。")
    public String listAvailableAgents(
            @ToolParam(description = "角色筛选：admin-管理员, agent-处理人, user-普通用户") String role,
            @ToolParam(description = "部门筛选") String department) {
        log.info("Tool: listAvailableAgents - role: {}, department: {}", role, department);
        return workOrderBridge.listAvailableAgents(role, department);
    }
}
