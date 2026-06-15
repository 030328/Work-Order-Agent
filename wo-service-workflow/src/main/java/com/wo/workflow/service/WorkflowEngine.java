package com.wo.workflow.service;

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

    /**
     * Transition request DTO.
     */
    @lombok.Data
    class TransitionRequest {
        private Long workOrderId;
        private Long definitionId;
        private String fromState;
        private String toState;
        private String event;
        private Long operatorId;
        private String operatorRole;
        private java.util.Map<String, Object> context;
    }

    /**
     * Transition result DTO.
     */
    @lombok.Data
    class TransitionResult {
        private boolean success;
        private String message;
        private String newState;

        public static TransitionResult success(String newState) {
            TransitionResult result = new TransitionResult();
            result.setSuccess(true);
            result.setMessage("Transition executed successfully");
            result.setNewState(newState);
            return result;
        }

        public static TransitionResult failure(String message) {
            TransitionResult result = new TransitionResult();
            result.setSuccess(false);
            result.setMessage(message);
            return result;
        }
    }
}
