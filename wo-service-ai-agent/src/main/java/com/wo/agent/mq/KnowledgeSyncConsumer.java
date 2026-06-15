package com.wo.agent.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wo.agent.entity.KbDocument;
import com.wo.agent.feign.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 知识库同步消费者
 * 监听工单关闭事件，自动将工单内容向量化存入知识库
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "wo-close",
        consumerGroup = "${rocketmq.consumer.group}"
)
public class KnowledgeSyncConsumer implements RocketMQListener<String> {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(String message) {
        log.info("Received work order close event: {}", message);

        try {
            // 解析消息
            JsonNode jsonNode = objectMapper.readTree(message);
            Long workOrderId = jsonNode.get("workOrderId").asLong();
            String title = jsonNode.has("title") ? jsonNode.get("title").asText() : "工单 #" + workOrderId;
            String description = jsonNode.has("description") ? jsonNode.get("description").asText() : "";
            String solution = jsonNode.has("solution") ? jsonNode.get("solution").asText() : "";
            String category = jsonNode.has("category") ? jsonNode.get("category").asText() : "other";

            // 构建知识库文档内容
            StringBuilder content = new StringBuilder();
            content.append("工单标题: ").append(title).append("\n\n");
            if (!description.isEmpty()) {
                content.append("问题描述:\n").append(description).append("\n\n");
            }
            if (!solution.isEmpty()) {
                content.append("解决方案:\n").append(solution).append("\n\n");
            }

            // 创建知识库文档
            KbDocument document = new KbDocument();
            document.setTitle("[工单] " + title);
            document.setContent(content.toString());
            document.setSourceType("workorder");
            document.setSourceId(String.valueOf(workOrderId));
            document.setCategory(category);
            document.setStatus(1);

            // 保存文档并触发索引
            Long docId = knowledgeBaseService.createDocument(document);
            knowledgeBaseService.triggerIndex(docId);

            log.info("Work order {} synced to knowledge base as document {}", workOrderId, docId);

        } catch (Exception e) {
            log.error("Failed to sync work order to knowledge base", e);
            // 不抛出异常，避免重复消费
        }
    }
}
