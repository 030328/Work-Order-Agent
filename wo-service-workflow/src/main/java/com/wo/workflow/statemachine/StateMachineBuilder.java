package com.wo.workflow.statemachine;

import com.wo.workflow.entity.WfDefinition;
import com.wo.workflow.entity.WfTransition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds a state machine representation from workflow definition and transitions.
 */
@Slf4j
@Component
public class StateMachineBuilder {

    /**
     * Build a state machine from the given definition and transitions.
     *
     * @param definition  the workflow definition
     * @param transitions the list of transitions
     * @return the built state machine
     */
    public StateMachine build(WfDefinition definition, List<WfTransition> transitions) {
        log.debug("Building state machine for definition: {}", definition.getName());

        StateMachine sm = new StateMachine();
        sm.setDefinitionId(definition.getId());
        sm.setName(definition.getName());
        sm.setVersion(definition.getVersion());

        // Collect all states
        Set<String> states = new HashSet<>();
        Map<String, List<WfTransition>> transitionMap = new HashMap<>();

        for (WfTransition t : transitions) {
            states.add(t.getFromState());
            states.add(t.getToState());
            transitionMap.computeIfAbsent(t.getFromState(), k -> new ArrayList<>()).add(t);
        }

        sm.setStates(states);
        sm.setTransitions(transitionMap);

        log.debug("Built state machine with {} states and {} transitions", states.size(), transitions.size());
        return sm;
    }

    /**
     * Simple state machine representation.
     */
    @lombok.Data
    public static class StateMachine {
        private Long definitionId;
        private String name;
        private Integer version;
        private Set<String> states = new HashSet<>();
        private Map<String, List<WfTransition>> transitions = new HashMap<>();

        /**
         * Get all available transitions from a given state.
         *
         * @param fromState the source state
         * @return list of transitions from the state
         */
        public List<WfTransition> getTransitionsFrom(String fromState) {
            return transitions.getOrDefault(fromState, Collections.emptyList());
        }

        /**
         * Check if a transition from one state to another is valid.
         *
         * @param fromState source state
         * @param toState   target state
         * @return true if the transition exists
         */
        public boolean canTransition(String fromState, String toState) {
            return getTransitionsFrom(fromState).stream()
                    .anyMatch(t -> t.getToState().equals(toState));
        }

        /**
         * Get all target states reachable from the given state.
         *
         * @param fromState the source state
         * @return set of reachable target states
         */
        public Set<String> getReachableStates(String fromState) {
            return getTransitionsFrom(fromState).stream()
                    .map(WfTransition::getToState)
                    .collect(Collectors.toSet());
        }

        /**
         * Check if a state is valid (exists in the state machine).
         *
         * @param state the state to check
         * @return true if the state exists
         */
        public boolean isValidState(String state) {
            return states.contains(state);
        }
    }
}
