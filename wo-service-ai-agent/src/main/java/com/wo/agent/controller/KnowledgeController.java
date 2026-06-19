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
        ragService.index(request.getContent(), request.getSourceType(), request.getSourceId());
        return R.ok("索引成功");
    }

    @PostMapping("/search")
    public R<?> search(@RequestBody SearchRequest request) {
        var results = ragService.retrieve(request.getQuery(), request.getTopK());
        return R.ok(results);
    }

    @Data
    static class IndexRequest {
        private String content;
        private String sourceType;
        private String sourceId;
    }

    @Data
    static class SearchRequest {
        private String query;
        private int topK = 3;
    }
}
