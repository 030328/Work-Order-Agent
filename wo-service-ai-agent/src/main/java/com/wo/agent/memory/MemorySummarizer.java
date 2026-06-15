package com.wo.agent.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 记忆摘要服务
 * 当对话历史过长时，使用 LLM 生成摘要以压缩记忆
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemorySummarizer {

    private final ChatClient.Builder chatClientBuilder;

    private static final String SUMMARIZE_PROMPT = """
            请将以下对话历史总结为简洁的上下文摘要。保留关于工单的关键信息、已做出的决定和已采取的行动。

            对话历史：
            %s

            请提供简要摘要（不超过200字）：
            """;

    /**
     * 对历史消息生成摘要
     *
     * @param oldMessages 需要摘要的历史消息
     * @return 摘要文本
     */
    public String summarize(List<String> oldMessages) {
        if (oldMessages == null || oldMessages.isEmpty()) {
            return "";
        }

        try {
            // 构建历史消息文本
            StringBuilder historyBuilder = new StringBuilder();
            for (String message : oldMessages) {
                historyBuilder.append(message).append("\n");
            }

            String prompt = String.format(SUMMARIZE_PROMPT, historyBuilder.toString());

            // 调用 LLM 生成摘要（使用轻量级模型）
            ChatClient chatClient = chatClientBuilder.build();
            String summary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("Summarized {} messages into {} chars", oldMessages.size(),
                    summary != null ? summary.length() : 0);

            return summary != null ? summary : "";

        } catch (Exception e) {
            log.error("Failed to summarize messages", e);
            // 降级策略：取最后几条消息作为摘要
            return fallbackSummary(oldMessages);
        }
    }

    /**
     * 降级摘要策略：取最后几条消息
     */
    private String fallbackSummary(List<String> messages) {
        int startIndex = Math.max(0, messages.size() - 5);
        StringBuilder sb = new StringBuilder("[历史摘要]\n");
        for (int i = startIndex; i < messages.size(); i++) {
            sb.append(messages.get(i)).append("\n");
        }
        return sb.toString();
    }
}
