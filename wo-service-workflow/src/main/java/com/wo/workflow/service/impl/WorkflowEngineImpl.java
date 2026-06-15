package com.wo.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        log.info("Executing transition for workOrderId={}, fromState={}, toState={}, event={}",
                request.getWorkOrderId(), request.getFromState(), request.getToState(), request.getEvent());

        // Load the workflow definition
        WfDefinition definition = definitionMapper.selectById(request.getDefinitionId());
        if (definition == null || definition.getStatus() != 1) {
            return TransitionResult.failure("Workflow definition not found or inactive");
        }

        // Find the matching transition
        LambdaQueryWrapper<WfTransition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTransition::getDefinitionId, request.getDefinitionId())
                .eq(WfTransition::getFromState, request.getFromState())
                .eq(WfTransition::getToState, request.getToState())
                .orderByAsc(WfTransition::getSortOrder);
        List<WfTransition> transitions = transitionMapper.selectList(wrapper);

        WfTransition matchedTransition = transitions.stream()
                .filter(t -> request.getEvent() == null || request.getEvent().equals(t.getEvent()))
                .findFirst()
                .orElse(null);

        if (matchedTransition == null) {
            return TransitionResult.failure("No matching transition found from " + request.getFromState() + " to " + request.getToState());
        }

        // Evaluate guard condition
        if (matchedTransition.getGuardCondition() != null && !matchedTransition.getGuardCondition().isEmpty()) {
            Map<String, Object> context = request.getContext() != null ? request.getContext() : new java.util.HashMap<>();
            context.put("workOrderId", request.getWorkOrderId());
            context.put("operatorId", request.getOperatorId());
            context.put("operatorRole", request.getOperatorRole());

            boolean guardPassed = guardEvaluator.evaluate(matchedTransition.getGuardCondition(), context);
            if (!guardPassed) {
                return TransitionResult.failure("Guard condition not met: " + matchedTransition.getGuardCondition());
            }
        }

        // Validate required role
        if (matchedTransition.getRequiredRole() != null && !matchedTransition.getRequiredRole().isEmpty()) {
            if (request.getOperatorRole() == null || !matchedTransition.getRequiredRole().equals(request.getOperatorRole())) {
                return TransitionResult.failure("Required role not satisfied: " + matchedTransition.getRequiredRole());
            }
        }

        // Execute action if specified
        if (matchedTransition.getActionClass() != null && !matchedTransition.getActionClass().isEmpty()) {
            try {
                Map<String, Object> context = request.getContext() != null ? request.getContext() : new java.util.HashMap<>();
                context.put("workOrderId", request.getWorkOrderId());
                context.put("fromState", request.getFromState());
                context.put("toState", request.getToState());
                context.put("operatorId", request.getOperatorId());
                actionExecutor.execute(matchedTransition.getActionClass(), context);
            } catch (Exception e) {
                log.error("Action execution failed for transition: {}", matchedTransition.getActionClass(), e);
                return TransitionResult.failure("Action execution failed: " + e.getMessage());
            }
        }

        log.info("Transition executed successfully for workOrderId={}, newState={}", request.getWorkOrderId(), request.getToState());
        return TransitionResult.success(request.getToState());
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
}
