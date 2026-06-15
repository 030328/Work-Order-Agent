package com.wo.agent.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG（检索增强生成）服务
 * 负责协调 Embedding 和向量检索，为 AI Agent 提供知识库上下文
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService embeddingService;
    private final MilvusClientWrapper milvusClient;

    @Value("${milvus.collection:wo_knowledge_vectors}")
    private String collectionName;

    @Value("${agent.rag.top-k:5}")
    private int defaultTopK;

    /**
     * 检索与查询相关的知识库内容
     *
     * 流程：
     * 1. 将查询文本转换为向量
     * 2. 在 Milvus 中进行相似度搜索
     * 3. 返回最相关的文档片段
     *
     * @param query 查询文本
     * @param topK  返回结果数量
     * @return 相关知识片段列表
     */
    public List<KnowledgeSearchResult> retrieve(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        if (topK <= 0) {
            topK = defaultTopK;
        }

        try {
            // Step 1: 文本向量化
            float[] queryVector = embeddingService.embed(query);
            if (queryVector == null || queryVector.length == 0) {
                log.warn("Failed to embed query: {}", query);
                return Collections.emptyList();
            }

            // Step 2: 向量检索
            List<MilvusClientWrapper.SearchResult> searchResults =
                    milvusClient.search(collectionName, queryVector, topK);

            // Step 3: 转换结果
            return searchResults.stream()
                    .map(this::convertToKnowledgeResult)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("RAG retrieve failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 转换 Milvus 搜索结果为知识搜索结果
     */
    private KnowledgeSearchResult convertToKnowledgeResult(MilvusClientWrapper.SearchResult searchResult) {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setId(searchResult.getId());
        result.setContent(searchResult.getContent());
        result.setTitle(searchResult.getMetadata().getOrDefault("title", "无标题"));
        result.setSourceId(searchResult.getMetadata().get("sourceId"));
        result.setSourceType(searchResult.getMetadata().get("sourceType"));
        result.setScore(searchResult.getScore());
        return result;
    }
}
