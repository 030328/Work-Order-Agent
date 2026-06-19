package com.wo.agent.rag;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    private static final String EMBEDDING_MODEL = "text-embedding-v3";

    public float[] embed(String text) {
        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(apiKey)
                    .model(EMBEDDING_MODEL)
                    .texts(Collections.singletonList(text))
                    .build();

            TextEmbedding embedding = new TextEmbedding();
            TextEmbeddingResult result = embedding.call(param);

            List<Double> vector = result.getOutput().getEmbeddings().get(0).getEmbedding();
            float[] array = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                array[i] = vector.get(i).floatValue();
            }
            return array;
        } catch (Exception e) {
            log.error("Embedding failed", e);
            return null;
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(apiKey)
                    .model(EMBEDDING_MODEL)
                    .texts(texts)
                    .build();

            TextEmbedding embedding = new TextEmbedding();
            TextEmbeddingResult result = embedding.call(param);

            List<float[]> vectors = new ArrayList<>();
            for (var emb : result.getOutput().getEmbeddings()) {
                List<Double> vector = emb.getEmbedding();
                float[] array = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    array[i] = vector.get(i).floatValue();
                }
                vectors.add(array);
            }
            return vectors;
        } catch (Exception e) {
            log.error("Batch embedding failed", e);
            return List.of();
        }
    }
}
