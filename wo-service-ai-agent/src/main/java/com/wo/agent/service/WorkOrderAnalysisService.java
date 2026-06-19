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
import java.util.stream.Collectors;

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

        // 1. RAG: 检索相似历史工单
        String ragContext = buildRagContext(request);

        // 2. 构建 Prompt（注入 RAG 上下文）
        String prompt = buildPrompt(request, ragContext);

        try {
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一个专业的工单分析助手，擅长分析工单内容并提供建议。" +
                            "如果有相似的历史工单和解决方案，请参考它们给出更准确的建议。")
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
                    .maxTokens(800)
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

    private String buildRagContext(WorkOrderAnalysisRequest request) {
        try {
            String query = request.getTitle() + " " + request.getDescription();
            List<KnowledgeSearchResult> results = ragService.retrieve(query, 3);

            if (results.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder("\n\n【相似历史工单参考】\n");
            for (int i = 0; i < results.size(); i++) {
                KnowledgeSearchResult r = results.get(i);
                context.append(String.format("相似工单%d (相似度: %.2f):\n", i + 1, r.getScore()));
                context.append("内容: ").append(r.getContent()).append("\n");
                if (r.getSourceType() != null) {
                    context.append("来源: ").append(r.getSourceType()).append("\n");
                }
                context.append("\n");
            }

            return context.toString();
        } catch (Exception e) {
            log.warn("RAG retrieval failed, continue without context", e);
            return "";
        }
    }

    private String buildPrompt(WorkOrderAnalysisRequest request, String ragContext) {
        return """
                你是一个工单分析助手。请分析以下工单信息，返回分析结果。

                工单标题：%s
                工单描述：%s
                %s
                请以JSON格式返回分析结果，包含以下字段：
                {
                  "suggestedCategory": "建议分类(BUG/FEATURE/QUESTION/MAINTENANCE/INCIDENT)",
                  "suggestedPriority": "建议优先级(LOW/MEDIUM/HIGH/URGENT)",
                  "summary": "工单摘要(50字以内)",
                  "sentiment": "用户情绪(POSITIVE/NEUTRAL/NEGATIVE)",
                  "suggestedSolution": "建议解决方案(100字以内，可参考相似历史工单的解决方案)"
                }

                只返回JSON，不要其他内容。
                """.formatted(request.getTitle(), request.getDescription(), ragContext);
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
