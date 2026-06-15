package com.wo.agent.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量化服务
 * 调用 DashScope Embedding API 将文本转换为向量
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * 将单个文本转换为向量
     *
     * @param text 待向量化的文本
     * @return 向量数组
     */
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[0];
        }

        try {
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            EmbeddingResponse response = embeddingModel.call(request);

            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                float[] embedding = response.getResults().get(0).getOutput();
                log.debug("Embedded text ({} chars) -> {} dimensions", text.length(), embedding.length);
                return embedding;
            }

            log.warn("Empty embedding response for text: {}", text.substring(0, Math.min(50, text.length())));
            return new float[0];

        } catch (Exception e) {
            log.error("Embedding failed for text: {}", text.substring(0, Math.min(50, text.length())), e);
            return new float[0];
        }
    }

    /**
     * 批量文本向量化
     *
     * @param texts 待向量化的文本列表
     * @return 向量数组列表
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            EmbeddingRequest request = new EmbeddingRequest(texts, null);
            EmbeddingResponse response = embeddingModel.call(request);

            if (response != null && response.getResults() != null) {
                List<float[]> embeddings = response.getResults().stream()
                        .map(result -> result.getOutput())
                        .collect(Collectors.toList());
                log.debug("Batch embedded {} texts -> {} vectors", texts.size(), embeddings.size());
                return embeddings;
            }

            log.warn("Empty batch embedding response for {} texts", texts.size());
            return List.of();

        } catch (Exception e) {
            log.error("Batch embedding failed for {} texts", texts.size(), e);
            return List.of();
        }
    }
}
