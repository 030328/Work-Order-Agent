package com.wo.agent.controller;

import com.wo.agent.entity.KbDocument;
import com.wo.agent.feign.KnowledgeBaseService;
import com.wo.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库文档
     */
    @PostMapping("/documents")
    public Result<Long> createDocument(@RequestBody KbDocument document) {
        log.info("Creating knowledge document: {}", document.getTitle());
        Long docId = knowledgeBaseService.createDocument(document);
        return Result.success(docId);
    }

    /**
     * 查询知识库文档列表
     */
    @GetMapping("/documents")
    public Result<List<KbDocument>> listDocuments(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        log.info("Listing knowledge documents - category: {}, status: {}", category, status);
        List<KbDocument> documents = knowledgeBaseService.listDocuments(category, status);
        return Result.success(documents);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/documents/{id}")
    public Result<KbDocument> getDocument(@PathVariable Long id) {
        log.info("Getting knowledge document: {}", id);
        KbDocument document = knowledgeBaseService.getDocument(id);
        if (document == null) {
            return Result.fail("文档不存在");
        }
        return Result.success(document);
    }

    /**
     * 触发文档索引
     */
    @PostMapping("/documents/{id}/index")
    public Result<Void> triggerIndex(@PathVariable Long id) {
        log.info("Triggering index for document: {}", id);
        knowledgeBaseService.triggerIndex(id);
        return Result.success();
    }

    /**
     * 删除知识库文档
     */
    @DeleteMapping("/documents/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        log.info("Deleting knowledge document: {}", id);
        knowledgeBaseService.deleteDocument(id);
        return Result.success();
    }

    /**
     * 触发全量重新索引
     */
    @PostMapping("/reindex")
    public Result<Void> triggerReindexAll() {
        log.info("Triggering full reindex");
        knowledgeBaseService.triggerReindexAll();
        return Result.success();
    }
}
