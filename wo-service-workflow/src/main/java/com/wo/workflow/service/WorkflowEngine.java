package com.wo.workflow.service;

import com.wo.api.dto.workflow.TransitionRequest;
import com.wo.api.dto.workflow.TransitionResult;
import com.wo.workflow.entity.WfTransition;

import java.util.List;

/**
 * Core workflow engine interface for executing and validating state transitions.
 */
public interface WorkflowEngine {

    /**
     * Execute a state transition for a work order.
     *
     * @param request transition request containing workOrderId, fromState, toState, event, operatorId, operatorRole
     * @return transition result with success flag and message
     */
    TransitionResult executeTransition(TransitionRequest request);

    /**
     * Validate whether a transition is allowed from the current state to the target state.
     *
     * @param definitionId workflow definition ID
     * @param fromState    current state
     * @param toState      target state
     * @return true if the transition is valid
     */
    boolean validateTransition(Long definitionId, String fromState, String toState);

    /**
     * Get all available target states from the current state for a given workflow definition.
     *
     * @param definitionId workflow definition ID
     * @param currentState current state
     * @return list of available target state names
     */
    List<String> getAvailableTransitions(Long definitionId, String currentState);

}
