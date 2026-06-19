package com.wo.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.api.dto.workflow.TransitionRequest;
import com.wo.api.dto.workflow.TransitionResult;
import com.wo.workflow.entity.WfDefinition;
import com.wo.workflow.entity.WfTransition;
import com.wo.workflow.mapper.TransitionMapper;
import com.wo.workflow.mapper.WorkflowDefinitionMapper;
import com.wo.workflow.service.WorkflowEngine;
import com.wo.workflow.statemachine.ActionExecutor;
import com.wo.workflow.statemachine.GuardEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the workflow engine.
 * Loads definitions from DB, validates transitions, evaluates guard conditions,
 * checks required roles, and executes transition actions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngineImpl implements WorkflowEngine {

    private final WorkflowDefinitionMapper definitionMapper;
    private final TransitionMapper transitionMapper;
    private final GuardEvaluator guardEvaluator;
    private final ActionExecutor actionExecutor;

    @Override
    public TransitionResult executeTransition(TransitionRequest request) {
        log.info("Executing transition for workOrderId={}, fromStatus={}, toStatus={}, event={}",
                request.getWorkOrderId(), request.getFromStatus(), request.getToStatus(), request.getEvent());

        WfDefinition definition = loadDefinition(request.getDefinitionId());
        if (definition == null || definition.getStatus() != 1) {
            return TransitionResult.failure(request.getFromStatus(), request.getToStatus(),
                    "Workflow definition not found or inactive");
        }

        LambdaQueryWrapper<WfTransition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTransition::getDefinitionId, definition.getId())
                .eq(WfTransition::getFromState, request.getFromStatus())
                .eq(WfTransition::getToState, request.getToStatus())
                .orderByAsc(WfTransition::getSortOrder);
        List<WfTransition> transitions = transitionMapper.selectList(wrapper);

        WfTransition matchedTransition = transitions.stream()
                .filter(t -> request.getEvent() == null || request.getEvent().equals(t.getEvent()))
                .findFirst()
                .orElse(null);

        if (matchedTransition == null) {
            return TransitionResult.failure(request.getFromStatus(), request.getToStatus(),
                    "No matching transition found from " + request.getFromStatus() + " to " + request.getToStatus());
        }

        // Evaluate guard condition
        if (matchedTransition.getGuardCondition() != null && !matchedTransition.getGuardCondition().isEmpty()) {
            Map<String, Object> context = new java.util.HashMap<>();
            context.put("workOrderId", request.getWorkOrderId());
            context.put("operatorId", request.getOperatorId());
            context.put("operatorRole", request.getOperatorRole());
            context.put("fromStatus", request.getFromStatus());
            context.put("toStatus", request.getToStatus());

            boolean guardPassed = guardEvaluator.evaluate(matchedTransition.getGuardCondition(), context);
            if (!guardPassed) {
                return TransitionResult.failure(request.getFromStatus(), request.getToStatus(),
                        "Guard condition not met: " + matchedTransition.getGuardCondition());
            }
        }

        // Validate required role
        if (matchedTransition.getRequiredRole() != null && !matchedTransition.getRequiredRole().isEmpty()) {
            if (!hasRequiredRole(request.getOperatorRole(), matchedTransition.getRequiredRole())) {
                return TransitionResult.failure(request.getFromStatus(), request.getToStatus(),
                        "Required role not satisfied: " + matchedTransition.getRequiredRole());
            }
        }

        // Execute action if specified
        if (matchedTransition.getActionClass() != null && !matchedTransition.getActionClass().isEmpty()) {
            try {
                Map<String, Object> context = new java.util.HashMap<>();
                context.put("workOrderId", request.getWorkOrderId());
                context.put("fromState", request.getFromStatus());
                context.put("toState", request.getToStatus());
                context.put("operatorId", request.getOperatorId());
                actionExecutor.execute(matchedTransition.getActionClass(), context);
            } catch (Exception e) {
                log.error("Action execution failed for transition: {}", matchedTransition.getActionClass(), e);
                return TransitionResult.failure(request.getFromStatus(), request.getToStatus(),
                        "Action execution failed: " + e.getMessage());
            }
        }

        log.info("Transition executed successfully for workOrderId={}, newState={}", request.getWorkOrderId(), request.getToStatus());
        return TransitionResult.success(request.getFromStatus(), request.getToStatus());
    }

    @Override
    public boolean validateTransition(Long definitionId, String fromState, String toState) {
        LambdaQueryWrapper<WfTransition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTransition::getDefinitionId, definitionId)
                .eq(WfTransition::getFromState, fromState)
                .eq(WfTransition::getToState, toState);
        return transitionMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<String> getAvailableTransitions(Long definitionId, String currentState) {
        LambdaQueryWrapper<WfTransition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTransition::getDefinitionId, definitionId)
                .eq(WfTransition::getFromState, currentState)
                .orderByAsc(WfTransition::getSortOrder);
        List<WfTransition> transitions = transitionMapper.selectList(wrapper);
        return transitions.stream()
                .map(WfTransition::getToState)
                .distinct()
                .collect(Collectors.toList());
    }

    private WfDefinition loadDefinition(Long definitionId) {
        if (definitionId != null) {
            return definitionMapper.selectById(definitionId);
        }

        LambdaQueryWrapper<WfDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDefinition::getStatus, 1)
                .orderByDesc(WfDefinition::getVersion)
                .last("LIMIT 1");
        return definitionMapper.selectOne(wrapper);
    }

    private boolean hasRequiredRole(String operatorRole, String requiredRole) {
        if (operatorRole == null || operatorRole.isEmpty()) {
            return false;
        }
        return roleLevel(operatorRole) >= roleLevel(requiredRole);
    }

    private int roleLevel(String role) {
        return switch (role) {
            case "ADMIN" -> 4;
            case "MANAGER" -> 3;
            case "AGENT" -> 2;
            case "USER" -> 1;
            default -> 0;
        };
    }
}
