package com.wo.agent.rag;

import com.wo.api.dto.ai.KnowledgeSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
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

        // 2. Search Milvus (多取一些，后面排序)
        List<Map<String, Object>> results = milvusClient.search(queryVector, topK * 2);
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
                    .verified(result.get("verified") != null ? ((Number) result.get("verified")).intValue() : 0)
                    .likeCount(result.get("like_count") != null ? ((Number) result.get("like_count")).intValue() : 0)
                    .build();
            searchResults.add(item);
        }

        // 4. 排序：verified=1 优先，然后按 score 降序，再按 likeCount 降序
        searchResults.sort(Comparator
                .comparingInt(KnowledgeSearchResult::getVerified).reversed()
                .thenComparingDouble(KnowledgeSearchResult::getScore).reversed()
                .thenComparingInt(KnowledgeSearchResult::getLikeCount).reversed());

        // 5. 取 topK
        List<KnowledgeSearchResult> topResults = searchResults.size() > topK
                ? searchResults.subList(0, topK)
                : searchResults;

        log.info("RAG retrieved {} results (sorted by verified + score + likes)", topResults.size());
        return topResults;
    }

    public void index(String content, String sourceType, String sourceId) {
        index(content, sourceType, sourceId, 0, 0);
    }

    public void index(String content, String sourceType, String sourceId, int verified, int likeCount) {
        log.info("=== RAG Index Start ===");
        log.info("Content length: {}", content.length());
        log.info("SourceType: {}, SourceId: {}, Verified: {}, LikeCount: {}", sourceType, sourceId, verified, likeCount);

        // 1. Embed the content
        log.info("Step 1: Embedding content...");
        float[] vector = embeddingService.embed(content);
        if (vector == null) {
            log.error("Step 1 FAILED: Embedding returned null, skip indexing");
            return;
        }
        log.info("Step 1 SUCCESS: Vector dimension={}", vector.length);

        // 2. Insert into Milvus
        log.info("Step 2: Inserting into Milvus...");
        milvusClient.insert(content, vector, sourceType, sourceId, verified, likeCount);
        log.info("=== RAG Index Complete ===");
    }
}
