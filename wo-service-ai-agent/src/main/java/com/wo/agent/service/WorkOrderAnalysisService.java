package com.wo.agent.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.wo.agent.rag.RagService;
import com.wo.api.dto.ai.KnowledgeSearchResult;
import com.wo.api.dto.ai.WorkOrderAnalysisRequest;
import com.wo.api.dto.ai.WorkOrderAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderAnalysisService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${agent.model:qwen-plus}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RagService ragService;

    public WorkOrderAnalysisResult analyze(WorkOrderAnalysisRequest request) {
        log.info("Analyzing work order: {}", request.getTitle());

        // 1. 构建 ES 相似工单上下文
        String esContext = buildEsContext(request.getSimilarWorkOrders());

        // 2. RAG 检索知识库
        String ragContext = buildRagContext(request);

        // 3. 构建 Prompt
        String prompt = buildPrompt(request, esContext, ragContext);

        try {
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一个专业的工单分析助手，擅长分析工单内容并提供建议。" +
                            "请参考历史相似工单和知识库文档，给出更准确的分析和建议。")
                    .build();

            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .maxTokens(1000)
                    .temperature(0.7F)
                    .build();

            Generation gen = new Generation();
            GenerationResult result = gen.call(param);

            String content = result.getOutput()
                    .getChoices().get(0)
                    .getMessage().getContent();

            log.info("AI response: {}", content);
            return parseResponse(content);

        } catch (Exception e) {
            log.error("AI analysis failed", e);
            return WorkOrderAnalysisResult.builder()
                    .suggestedCategory("QUESTION")
                    .suggestedPriority("MEDIUM")
                    .summary("AI分析失败，请人工处理")
                    .sentiment("NEUTRAL")
                    .suggestedSolution("暂无建议")
                    .build();
        }
    }

    /**
     * 构建 ES 相似工单上下文
     */
    private String buildEsContext(List<WorkOrderAnalysisRequest.SimilarWorkOrder> similarWorkOrders) {
        if (similarWorkOrders == null || similarWorkOrders.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("\n\n【相似历史工单（ES 检索 Top 5）】\n");
        for (int i = 0; i < similarWorkOrders.size(); i++) {
            WorkOrderAnalysisRequest.SimilarWorkOrder wo = similarWorkOrders.get(i);
            context.append(String.format("工单%d: %s\n", i + 1, wo.getTitle()));
            context.append(String.format("  编号: %s\n", wo.getOrderNo()));
            context.append(String.format("  描述: %s\n", wo.getDescription()));
            if (wo.getResolution() != null && !wo.getResolution().isEmpty()) {
                context.append(String.format("  解决方案: %s\n", wo.getResolution()));
            }
            context.append(String.format("  状态: %s\n\n", wo.getStatus()));
        }

        return context.toString();
    }

    /**
     * 构建 RAG 知识库上下文
     */
    private String buildRagContext(WorkOrderAnalysisRequest request) {
        try {
            String query = request.getTitle() + " " + request.getDescription();
            List<KnowledgeSearchResult> results = ragService.retrieve(query, 3);

            if (results.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder("\n\n【知识库参考（Milvus RAG 检索）】\n");
            for (int i = 0; i < results.size(); i++) {
                KnowledgeSearchResult r = results.get(i);
                context.append(String.format("文档%d (相似度: %.2f):\n", i + 1, r.getScore()));
                context.append("内容: ").append(r.getContent()).append("\n\n");
            }

            return context.toString();
        } catch (Exception e) {
            log.warn("RAG retrieval failed", e);
            return "";
        }
    }

    /**
     * 构建完整 Prompt
     */
    private String buildPrompt(WorkOrderAnalysisRequest request, String esContext, String ragContext) {
        return """
                请分析以下工单信息，返回分析结果。

                【当前工单】
                标题：%s
                描述：%s
                分类：%s
                优先级：%s
                %s%s
                请参考上述相似历史工单的解决方案和知识库文档，给出更准确的分析。

                请以JSON格式返回分析结果：
                {
                  "suggestedCategory": "建议分类(BUG/FEATURE/QUESTION/MAINTENANCE/INCIDENT)",
                  "suggestedPriority": "建议优先级(LOW/MEDIUM/HIGH/URGENT)",
                  "summary": "工单摘要(50字以内)",
                  "sentiment": "用户情绪(POSITIVE/NEUTRAL/NEGATIVE)",
                  "suggestedSolution": "建议解决方案(参考历史工单和知识库，100字以内)"
                }

                只返回JSON，不要其他内容。
                """.formatted(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getPriority(),
                esContext,
                ragContext
        );
    }

    private WorkOrderAnalysisResult parseResponse(String json) {
        try {
            String cleanJson = json.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            return objectMapper.readValue(cleanJson.trim(), WorkOrderAnalysisResult.class);
        } catch (Exception e) {
            log.warn("Failed to parse AI response, using defaults", e);
            return WorkOrderAnalysisResult.builder()
                    .suggestedCategory("QUESTION")
                    .suggestedPriority("MEDIUM")
                    .summary("解析失败")
                    .sentiment("NEUTRAL")
                    .suggestedSolution("暂无建议")
                    .build();
        }
    }
}
