package com.wo.workflow.controller;

import com.wo.workflow.service.WorkflowEngine;
import com.wo.workflow.service.WorkflowEngine.TransitionRequest;
import com.wo.workflow.service.WorkflowEngine.TransitionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public ResponseEntity<TransitionResult> executeTransition(@RequestBody TransitionRequest request) {
        TransitionResult result = workflowEngine.executeTransition(request);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * Validate whether a transition is allowed.
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateTransition(@RequestBody Map<String, Object> request) {
        Long definitionId = Long.valueOf(request.get("definitionId").toString());
        String fromState = (String) request.get("fromState");
        String toState = (String) request.get("toState");

        boolean valid = workflowEngine.validateTransition(definitionId, fromState, toState);
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "definitionId", definitionId,
                "fromState", fromState,
                "toState", toState
        ));
    }
}
