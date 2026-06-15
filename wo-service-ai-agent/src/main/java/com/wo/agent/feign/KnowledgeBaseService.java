package com.wo.agent.feign;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.agent.entity.KbDocument;
import com.wo.agent.mapper.KbDocumentMapper;
import com.wo.agent.rag.KnowledgeIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库服务
 * 提供知识库文档的 CRUD 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KbDocumentMapper documentMapper;
    private final KnowledgeIndexer knowledgeIndexer;

    /**
     * 创建知识库文档
     *
     * @param document 文档信息
     * @return 文档ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDocument(KbDocument document) {
        document.setStatus(1);
        document.setChunkCount(0);
        documentMapper.insert(document);
        log.info("Created knowledge document: {} - {}", document.getId(), document.getTitle());
        return document.getId();
    }

    /**
     * 更新知识库文档
     *
     * @param document 文档信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(KbDocument document) {
        documentMapper.updateById(document);
        log.info("Updated knowledge document: {}", document.getId());
    }

    /**
     * 删除知识库文档（同时删除向量索引）
     *
     * @param documentId 文档ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        // 删除向量索引
        knowledgeIndexer.removeDocumentIndex(documentId);

        // 删除文档记录
        documentMapper.deleteById(documentId);
        log.info("Deleted knowledge document: {}", documentId);
    }

    /**
     * 获取文档详情
     *
     * @param documentId 文档ID
     * @return 文档信息
     */
    public KbDocument getDocument(Long documentId) {
        return documentMapper.selectById(documentId);
    }

    /**
     * 查询文档列表
     *
     * @param category 分类（可选）
     * @param status   状态（可选）
     * @return 文档列表
     */
    public List<KbDocument> listDocuments(String category, Integer status) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();

        if (category != null && !category.isEmpty()) {
            wrapper.eq(KbDocument::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(KbDocument::getStatus, status);
        }

        wrapper.orderByDesc(KbDocument::getCreateTime);
        return documentMapper.selectList(wrapper);
    }

    /**
     * 触发文档索引
     *
     * @param documentId 文档ID
     */
    public void triggerIndex(Long documentId) {
        KbDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new RuntimeException("文档不存在: " + documentId);
        }
        knowledgeIndexer.indexDocument(document);
    }

    /**
     * 触发全量重新索引
     */
    public void triggerReindexAll() {
        knowledgeIndexer.reindexAll();
    }
}
