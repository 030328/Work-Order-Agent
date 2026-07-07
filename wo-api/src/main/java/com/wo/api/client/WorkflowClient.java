package com.wo.api.client;

import com.wo.api.client.fallback.WorkflowClientFallback;
import com.wo.api.config.InternalFeignConfig;
import com.wo.api.dto.workflow.TransitionRequest;
import com.wo.api.dto.workflow.TransitionResult;
import com.wo.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "wo-service-workflow", fallbackFactory = WorkflowClientFallback.class, configuration = InternalFeignConfig.class)
public interface WorkflowClient {

    @PostMapping("/api/workflow/transitions/execute")
    R<TransitionResult> executeTransition(@RequestBody TransitionRequest request);

    @PostMapping("/api/workflow/sla/assign")
    R<Void> assignSla(@RequestParam("workOrderId") Long workOrderId,
                      @RequestParam("priority") String priority);

    @GetMapping("/api/workflow/sla/deadline")
    R<String> calculateSlaDeadline(@RequestParam("priority") String priority);
}
