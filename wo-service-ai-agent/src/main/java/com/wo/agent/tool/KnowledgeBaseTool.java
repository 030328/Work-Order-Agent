package com.wo.agent.tool;

import com.wo.agent.rag.KnowledgeSearchResult;
import com.wo.agent.rag.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库搜索工具
 * 提供知识库语义搜索能力，供 AI Agent 通过 Tool Calling 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseTool {

    private final RagService ragService;

    /**
     * 搜索知识库
     * 使用语义搜索从知识库中查找相关信息
     */
    @Tool(description = "从知识库中搜索相关信息。当用户询问技术问题、" +
            "寻找解决方案或需要参考资料时调用此工具。支持自然语言查询。")
    public String searchKnowledgeBase(
            @ToolParam(description = "搜索查询，使用自然语言描述问题或关键词") String query,
            @ToolParam(description = "返回结果数量，默认5") int topK) {
        log.info("Tool: searchKnowledgeBase - query: {}, topK: {}", query, topK);

        if (topK <= 0) {
            topK = 5;
        }

        List<KnowledgeSearchResult> results = ragService.retrieve(query, topK);

        if (results.isEmpty()) {
            return "未找到相关知识库内容。";
        }

        StringBuilder sb = new StringBuilder("知识库搜索结果：\n\n");
        for (int i = 0; i < results.size(); i++) {
            KnowledgeSearchResult result = results.get(i);
            sb.append(String.format("[%d] %s (相关度: %.0f%%)\n",
                    i + 1, result.getTitle(), result.getScore() * 100));
            sb.append(result.getContent()).append("\n\n");
        }

        return sb.toString();
    }
}
