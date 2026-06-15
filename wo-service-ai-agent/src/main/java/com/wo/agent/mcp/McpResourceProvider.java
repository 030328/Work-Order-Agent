package com.wo.agent.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP 资源提供者
 * 暴露工单模板、状态定义、分类定义等为 MCP 资源
 * 供其他 AI 客户端查询使用
 */
@Slf4j
@Component
public class McpResourceProvider {

    /**
     * 工单状态定义
     */
    private static final Map<String, String> WORK_ORDER_STATUSES = Map.of(
            "open", "待处理 - 新创建的工单，等待分配",
            "in_progress", "处理中 - 已分配处理人，正在处理",
            "resolved", "已解决 - 问题已修复，等待确认",
            "closed", "已关闭 - 工单完成，归档"
    );

    /**
     * 工单优先级定义
     */
    private static final Map<String, String> PRIORITIES = Map.of(
            "low", "低优先级 - 不影响业务，可延后处理",
            "medium", "中优先级 - 部分功能受影响，需在24小时内处理",
            "high", "高优先级 - 核心功能受损，需在4小时内处理",
            "urgent", "紧急 - 系统宕机或严重安全问题，需立即处理"
    );

    /**
     * 工单分类定义
     */
    private static final Map<String, String> CATEGORIES = Map.of(
            "hardware", "硬件问题 - 服务器、终端设备、网络设备等硬件故障",
            "software", "软件问题 - 应用bug、系统错误、兼容性问题等",
            "network", "网络问题 - 网络连接、VPN、防火墙、DNS等",
            "access", "权限问题 - 账号申请、权限变更、密码重置等",
            "other", "其他 - 不属于以上分类的问题"
    );

    /**
     * 工单创建模板
     */
    private static final String WORK_ORDER_TEMPLATE = """
            {
                "title": "简明扼要的问题描述",
                "description": "详细描述：\\n1. 问题现象\\n2. 影响范围\\n3. 已尝试的解决方案",
                "priority": "low|medium|high|urgent",
                "category": "hardware|software|network|access|other"
            }
            """;

    /**
     * 获取工单状态定义
     */
    public Map<String, String> getStatusDefinitions() {
        log.debug("Providing work order status definitions");
        return WORK_ORDER_STATUSES;
    }

    /**
     * 获取优先级定义
     */
    public Map<String, String> getPriorityDefinitions() {
        log.debug("Providing priority definitions");
        return PRIORITIES;
    }

    /**
     * 获取分类定义
     */
    public Map<String, String> getCategoryDefinitions() {
        log.debug("Providing category definitions");
        return CATEGORIES;
    }

    /**
     * 获取工单创建模板
     */
    public String getWorkOrderTemplate() {
        log.debug("Providing work order template");
        return WORK_ORDER_TEMPLATE;
    }

    /**
     * 获取所有资源摘要（用于 MCP 资源列表）
     */
    public List<Map<String, String>> listResources() {
        return List.of(
                Map.of("uri", "mcp://workorder/statuses", "name", "工单状态定义", "type", "reference"),
                Map.of("uri", "mcp://workorder/priorities", "name", "优先级定义", "type", "reference"),
                Map.of("uri", "mcp://workorder/categories", "name", "工单分类定义", "type", "reference"),
                Map.of("uri", "mcp://workorder/template", "name", "工单创建模板", "type", "template")
        );
    }
}
