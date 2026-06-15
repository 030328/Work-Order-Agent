package com.wo.agent.tool;

import com.wo.agent.feign.WorkOrderToolFeignBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 数据分析工具
 * 提供工单统计分析能力，供 AI Agent 通过 Tool Calling 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisTool {

    private final WorkOrderToolFeignBridge workOrderBridge;

    /**
     * 分析工单趋势
     * 按时间段和分类统计工单数据
     */
    @Tool(description = "分析工单趋势和统计数据。当用户询问工单数量、处理效率、" +
            "分类分布等问题时调用。支持按时间段和分类筛选。")
    public String analyzeTrends(
            @ToolParam(description = "统计周期：today-今天, week-本周, month-本月, quarter-本季度, year-本年") String period,
            @ToolParam(description = "分类筛选：all-全部, hardware-硬件, software-软件, network-网络, access-权限") String category) {
        log.info("Tool: analyzeTrends - period: {}, category: {}", period, category);
        return workOrderBridge.analyzeTrends(period, category);
    }
}
