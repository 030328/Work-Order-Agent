package com.wo.agent.controller;

import com.wo.agent.entity.KbDocument;
import com.wo.agent.mapper.KbDocumentMapper;
import com.wo.agent.rag.RagService;
import com.wo.api.dto.ai.KnowledgeIndexRequest;
import com.wo.common.result.R;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final RagService ragService;
    private final KbDocumentMapper kbDocumentMapper;

    @PostMapping("/index")
    public R<String> indexDocument(@RequestBody KnowledgeIndexRequest request) {
        int verified = request.getVerified() != null ? request.getVerified() : 0;
        int likeCount = request.getLikeCount() != null ? request.getLikeCount() : 0;
        String sourceType = StringUtils.hasText(request.getSourceType())
                ? request.getSourceType()
                : "DOCUMENTATION";

        KbDocument document = new KbDocument();
        document.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : buildDefaultTitle(sourceType));
        document.setContent(request.getContent());
        document.setSourceType(sourceType);
        document.setSourceId(request.getSourceId());
        document.setCategory(request.getCategory());
        document.setStatus(1);
        document.setVerified(verified);
        document.setLikeCount(likeCount);
        kbDocumentMapper.insert(document);

        String sourceId = StringUtils.hasText(request.getSourceId())
                ? request.getSourceId()
                : String.valueOf(document.getId());
        if (!StringUtils.hasText(request.getSourceId())) {
            document.setSourceId(sourceId);
            kbDocumentMapper.updateById(document);
        }

        ragService.index(request.getContent(), sourceType, sourceId, verified, likeCount);
        return R.ok(String.valueOf(document.getId()));
    }

    @PostMapping("/search")
    public R<?> search(@RequestBody SearchRequest request) {
        var results = ragService.retrieve(request.getQuery(), request.getTopK());
        return R.ok(results);
    }

    @PostMapping("/verify")
    public R<String> verifyDocument(@RequestParam Long id) {
        KbDocument document = kbDocumentMapper.selectById(id);
        if (document == null) {
            return R.fail("知识库文档不存在");
        }
        document.setVerified(1);
        kbDocumentMapper.updateById(document);
        return R.ok("验证成功");
    }

    @PostMapping("/like")
    public R<String> likeDocument(@RequestParam Long id) {
        KbDocument document = kbDocumentMapper.selectById(id);
        if (document == null) {
            return R.fail("知识库文档不存在");
        }
        int likeCount = document.getLikeCount() == null ? 0 : document.getLikeCount();
        document.setLikeCount(likeCount + 1);
        kbDocumentMapper.updateById(document);
        return R.ok("点赞成功");
    }

    private String buildDefaultTitle(String sourceType) {
        return "知识库文档-" + sourceType + "-" + System.currentTimeMillis();
    }

    @Data
    static class SearchRequest {
        private String query;
        private int topK = 3;
    }
}
