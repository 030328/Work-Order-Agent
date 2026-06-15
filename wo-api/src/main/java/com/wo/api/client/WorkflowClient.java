package com.wo.api.client;

import com.wo.api.client.fallback.WorkflowClientFallback;
import com.wo.api.dto.workflow.TransitionRequest;
import com.wo.api.dto.workflow.TransitionResult;
import com.wo.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "wo-service-workflow", fallbackFactory = WorkflowClientFallback.class)
public interface WorkflowClient {

    @PostMapping("/api/workflow/transitions")
    R<TransitionResult> executeTransition(@RequestBody TransitionRequest request);

    @GetMapping("/api/workflow/transitions/validate")
    R<Void> validateTransition(@RequestParam("workOrderId") Long workOrderId,
                               @RequestParam("targetStatus") String targetStatus);
}
