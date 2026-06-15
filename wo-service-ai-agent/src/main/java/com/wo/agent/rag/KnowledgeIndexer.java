package com.wo.agent.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.agent.entity.KbDocument;
import com.wo.agent.entity.KbVectorMapping;
import com.wo.agent.mapper.KbDocumentMapper;
import com.wo.agent.mapper.KbVectorMappingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库索引服务
 * 负责文档的分块、向量化和存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIndexer {

    private final DocumentChunker documentChunker;
    private final EmbeddingService embeddingService;
    private final MilvusClientWrapper milvusClient;
    private final KbDocumentMapper documentMapper;
    private final KbVectorMappingMapper vectorMappingMapper;

    @Value("${milvus.collection:wo_knowledge_vectors}")
    private String collectionName;

    /**
     * 索引单个文档
     * 流程：分块 -> 向量化 -> 存入 Milvus -> 保存映射关系
     *
     * @param document 知识库文档
     */
    @Transactional(rollbackFor = Exception.class)
    public void indexDocument(KbDocument document) {
        log.info("Indexing document: {} - {}", document.getId(), document.getTitle());

        try {
            // Step 1: 文本分块
            List<String> chunks = documentChunker.chunk(document.getContent());
            if (chunks.isEmpty()) {
                log.warn("Document {} produced no chunks", document.getId());
                return;
            }

            // Step 2: 批量向量化
            List<float[]> vectors = embeddingService.embedBatch(chunks);
            if (vectors.size() != chunks.size()) {
                log.error("Vector count mismatch: {} chunks vs {} vectors", chunks.size(), vectors.size());
                return;
            }

            // Step 3: 删除该文档的旧向量（重新索引场景）
            milvusClient.deleteBySourceId(collectionName, String.valueOf(document.getId()));

            // Step 4: 批量插入向量和保存映射
            for (int i = 0; i < chunks.size(); i++) {
                String milvusId = UUID.randomUUID().toString();
                String chunkText = chunks.get(i);
                float[] vector = vectors.get(i);

                // 构建元数据
                Map<String, String> metadata = new HashMap<>();
                metadata.put("sourceId", String.valueOf(document.getId()));
                metadata.put("sourceType", document.getSourceType());
                metadata.put("title", document.getTitle());

                // 插入 Milvus
                milvusClient.insert(collectionName, milvusId, vector, chunkText, metadata);

                // 保存映射关系到 MySQL
                KbVectorMapping mapping = new KbVectorMapping();
                mapping.setDocumentId(document.getId());
                mapping.setChunkIndex(i);
                mapping.setChunkText(chunkText);
                mapping.setMilvusId(milvusId);
                mapping.setTokenCount(estimateTokens(chunkText));
                vectorMappingMapper.insert(mapping);
            }

            // Step 5: 更新文档分块数量
            document.setChunkCount(chunks.size());
            documentMapper.updateById(document);

            log.info("Document {} indexed successfully, {} chunks created", document.getId(), chunks.size());

        } catch (Exception e) {
            log.error("Failed to index document: {}", document.getId(), e);
            throw new RuntimeException("文档索引失败: " + document.getTitle(), e);
        }
    }

    /**
     * 重新索引所有文档
     */
    @Transactional(rollbackFor = Exception.class)
    public void reindexAll() {
        log.info("Starting full reindex...");

        // 查询所有启用的文档
        List<KbDocument> documents = documentMapper.selectList(
                new LambdaQueryWrapper<KbDocument>()
                        .eq(KbDocument::getStatus, 1));

        int successCount = 0;
        int failCount = 0;

        for (KbDocument document : documents) {
            try {
                indexDocument(document);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to reindex document: {}", document.getId(), e);
                failCount++;
            }
        }

        log.info("Reindex completed: {} success, {} failed, total {}", successCount, failCount, documents.size());
    }

    /**
     * 删除文档索引
     *
     * @param documentId 文档ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeDocumentIndex(Long documentId) {
        log.info("Removing index for document: {}", documentId);

        // 删除 Milvus 向量
        milvusClient.deleteBySourceId(collectionName, String.valueOf(documentId));

        // 删除映射记录
        vectorMappingMapper.delete(
                new LambdaQueryWrapper<KbVectorMapping>()
                        .eq(KbVectorMapping::getDocumentId, documentId));

        log.info("Document {} index removed", documentId);
    }

    /**
     * 估算 Token 数量（简单估算：中文约1.5字符/token，英文约4字符/token）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简化估算：按平均2字符/token
        return text.length() / 2;
    }
}
