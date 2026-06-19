package com.wo.api.client;

import com.wo.api.client.fallback.AiAgentClientFallback;
import com.wo.api.dto.ai.WorkOrderAnalysisRequest;
import com.wo.api.dto.ai.WorkOrderAnalysisResult;
import com.wo.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wo-service-ai-agent", fallbackFactory = AiAgentClientFallback.class)
public interface AiAgentClient {

    @PostMapping("/api/ai/analyze")
    R<WorkOrderAnalysisResult> analyzeWorkOrder(@RequestBody WorkOrderAnalysisRequest request);
}
