package com.wo.agent.agent;

import com.wo.agent.rag.KnowledgeSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提示词模板工具类
 * 负责构建系统提示词、RAG 上下文和记忆上下文
 */
@Component
public class PromptTemplate {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一个智能工单管理助手，帮助用户创建、查询、更新和管理工单。

            你的能力：
            1. 根据用户描述创建工单
            2. 搜索和筛选现有工单
            3. 更新工单状态和指派
            4. 从知识库搜索解决方案
            5. 分析工单趋势和统计
            6. 为工单添加评论

            始终保持专业、准确和有帮助。创建工单时，从用户描述中提取相关细节。
            搜索时，使用适当的筛选条件。

            %s

            可用工具：createWorkOrder, searchWorkOrders, getWorkOrderDetail,
            updateWorkOrderStatus, assignWorkOrder, addComment,
            searchKnowledgeBase, listAvailableAgents, analyzeTrends
            """;

    private static final String RAG_CONTEXT_HEADER = "\n相关知识库参考：\n";
    private static final String MEMORY_CONTEXT_HEADER = "\n对话历史：\n";

    /**
     * 构建完整的系统提示词
     *
     * @param context 额外上下文信息（RAG、记忆等）
     * @return 系统提示词
     */
    public String buildSystemPrompt(String context) {
        String contextSection = (context != null && !context.isEmpty())
                ? "当前上下文：\n" + context
                : "";
        return String.format(SYSTEM_PROMPT_TEMPLATE, contextSection);
    }

    /**
     * 构建 RAG 检索结果上下文
     *
     * @param results RAG 检索结果列表
     * @return 格式化的 RAG 上下文
     */
    public String buildRagContext(List<KnowledgeSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(RAG_CONTEXT_HEADER);
        for (int i = 0; i < results.size(); i++) {
            KnowledgeSearchResult result = results.get(i);
            sb.append(String.format("[%d] %s (相似度: %.2f)\n",
                    i + 1, result.getTitle(), result.getScore()));
            sb.append(result.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 构建对话历史上下文
     *
     * @param messages 历史消息列表
     * @return 格式化的记忆上下文
     */
    public String buildMemoryContext(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(MEMORY_CONTEXT_HEADER);
        // 最近的消息在前，取最近的对话
        int startIndex = Math.max(0, messages.size() - 20);
        for (int i = startIndex; i < messages.size(); i++) {
            sb.append(messages.get(i)).append("\n");
        }
        return sb.toString();
    }
}
