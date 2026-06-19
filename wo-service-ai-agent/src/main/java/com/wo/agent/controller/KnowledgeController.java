package com.wo.agent.controller;

import com.wo.agent.rag.RagService;
import com.wo.common.result.R;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final RagService ragService;

    @PostMapping("/index")
    public R<String> indexDocument(@RequestBody IndexRequest request) {
        ragService.index(request.getContent(), request.getSourceType(), request.getSourceId(),
                request.getVerified() != null ? request.getVerified() : 0,
                request.getLikeCount() != null ? request.getLikeCount() : 0);
        return R.ok("索引成功");
    }

    @PostMapping("/search")
    public R<?> search(@RequestBody SearchRequest request) {
        var results = ragService.retrieve(request.getQuery(), request.getTopK());
        return R.ok(results);
    }

    @PostMapping("/verify")
    public R<String> verifyDocument(@RequestParam Long id) {
        // 标记文档为人工验证通过
        // 实际应该更新 MySQL 和 Milvus
        return R.ok("验证成功");
    }

    @PostMapping("/like")
    public R<String> likeDocument(@RequestParam Long id) {
        // 点赞 +1
        // 实际应该更新 MySQL 和 Milvus
        return R.ok("点赞成功");
    }

    @Data
    static class IndexRequest {
        private String content;
        private String sourceType;
        private String sourceId;
        private Integer verified;   // 0=AI生成, 1=人工确认
        private Integer likeCount;  // 点赞数
    }

    @Data
    static class SearchRequest {
        private String query;
        private int topK = 3;
    }
}
