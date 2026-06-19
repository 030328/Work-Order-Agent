package com.wo.workflow.controller;

import com.wo.api.dto.workflow.TransitionRequest;
import com.wo.api.dto.workflow.TransitionResult;
import com.wo.common.result.R;
import com.wo.workflow.service.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for executing and validating workflow transitions.
 */
@RestController
@RequestMapping("/api/workflow/transitions")
@RequiredArgsConstructor
public class TransitionController {

    private final WorkflowEngine workflowEngine;

    /**
     * Execute a state transition.
     */
    @PostMapping("/execute")
    public R<TransitionResult> executeTransition(@RequestBody TransitionRequest request) {
        TransitionResult result = workflowEngine.executeTransition(request);
        return Boolean.TRUE.equals(result.getSuccess()) ? R.ok(result) : R.fail(result.getMessage());
    }

    /**
     * Validate whether a transition is allowed.
     */
    @PostMapping("/validate")
    public R<TransitionResult> validateTransition(@RequestBody TransitionRequest request) {
        Long definitionId = request.getDefinitionId() != null ? request.getDefinitionId() : 1L;
        boolean valid = workflowEngine.validateTransition(definitionId, request.getFromStatus(), request.getToStatus());
        TransitionResult result = valid
                ? TransitionResult.success(request.getFromStatus(), request.getToStatus())
                : TransitionResult.failure(request.getFromStatus(), request.getToStatus(), "No matching transition found");
        return valid ? R.ok(result) : R.fail(result.getMessage());
    }
}
