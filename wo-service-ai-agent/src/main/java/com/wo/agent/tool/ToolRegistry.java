package com.wo.agent.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具注册中心
 * 收集所有 @Tool 注解的工具 Bean，提供给 ChatClient 注册使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final WorkOrderTool workOrderTool;
    private final KnowledgeBaseTool knowledgeBaseTool;
    private final UserTool userTool;
    private final AnalysisTool analysisTool;

    /**
     * 获取所有工具对象，用于 ChatClient.defaultTools() 注册
     *
     * @return 工具对象列表
     */
    public List<Object> getAllTools() {
        List<Object> tools = List.of(
                workOrderTool,
                knowledgeBaseTool,
                userTool,
                analysisTool
        );
        log.debug("Registered {} tool beans", tools.size());
        return tools;
    }

    /**
     * 获取工单管理工具
     */
    public WorkOrderTool getWorkOrderTool() {
        return workOrderTool;
    }

    /**
     * 获取知识库工具
     */
    public KnowledgeBaseTool getKnowledgeBaseTool() {
        return knowledgeBaseTool;
    }

    /**
     * 获取用户工具
     */
    public UserTool getUserTool() {
        return userTool;
    }

    /**
     * 获取分析工具
     */
    public AnalysisTool getAnalysisTool() {
        return analysisTool;
    }
}
