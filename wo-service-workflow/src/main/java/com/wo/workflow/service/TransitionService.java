package com.wo.workflow.service;

import com.wo.workflow.entity.WfDefinition;
import com.wo.workflow.entity.WfTransition;

import java.util.List;

/**
 * Service for managing workflow definitions and transitions.
 */
public interface TransitionService {

    /**
     * Create a new workflow definition.
     *
     * @param definition the workflow definition to create
     */
    void createDefinition(WfDefinition definition);

    /**
     * Get the currently active workflow definition.
     *
     * @return the active definition, or null if none exists
     */
    WfDefinition getActiveDefinition();

    /**
     * Get all transitions from a specific state for a given definition.
     *
     * @param definitionId workflow definition ID
     * @param fromState    source state
     * @return list of transitions from the specified state
     */
    List<WfTransition> getTransitions(Long definitionId, String fromState);
}
