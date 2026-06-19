package com.wo.agent.rag;

import com.wo.api.dto.ai.KnowledgeSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService embeddingService;
    private final MilvusClientWrapper milvusClient;

    private static final int DEFAULT_TOP_K = 3;

    public List<KnowledgeSearchResult> retrieve(String query) {
        return retrieve(query, DEFAULT_TOP_K);
    }

    public List<KnowledgeSearchResult> retrieve(String query, int topK) {
        log.info("RAG retrieve: query={}, topK={}", query, topK);

        // 1. Embed the query
        float[] queryVector = embeddingService.embed(query);
        if (queryVector == null) {
            log.warn("Failed to embed query, skip RAG");
            return List.of();
        }

        // 2. Search Milvus
        List<Map<String, Object>> results = milvusClient.search(queryVector, topK);
        if (results.isEmpty()) {
            log.info("No similar documents found");
            return List.of();
        }

        // 3. Convert to KnowledgeSearchResult
        List<KnowledgeSearchResult> searchResults = new ArrayList<>();
        for (Map<String, Object> result : results) {
            KnowledgeSearchResult item = KnowledgeSearchResult.builder()
                    .id(result.get("id") != null ? ((Number) result.get("id")).longValue() : null)
                    .content((String) result.get("content"))
                    .sourceType((String) result.get("source_type"))
                    .sourceId((String) result.get("source_id"))
                    .score(result.get("score") != null ? ((Number) result.get("score")).doubleValue() : 0.0)
                    .build();
            searchResults.add(item);
        }

        log.info("RAG retrieved {} results", searchResults.size());
        return searchResults;
    }

    public void index(String content, String sourceType, String sourceId) {
        log.info("Indexing document: sourceType={}, sourceId={}", sourceType, sourceId);

        // 1. Embed the content
        float[] vector = embeddingService.embed(content);
        if (vector == null) {
            log.error("Failed to embed content, skip indexing");
            return;
        }

        // 2. Insert into Milvus
        milvusClient.insert(content, vector, sourceType, sourceId);
        log.info("Document indexed successfully");
    }
}
