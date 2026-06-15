package com.wo.workflow.statemachine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.workflow.entity.WfDefinition;
import com.wo.workflow.entity.WfTransition;
import com.wo.workflow.mapper.TransitionMapper;
import com.wo.workflow.mapper.WorkflowDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for building and caching state machines from database definitions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StateMachineFactory {

    private final WorkflowDefinitionMapper definitionMapper;
    private final TransitionMapper transitionMapper;
    private final StateMachineBuilder builder;

    /**
     * Cache of state machines keyed by definition ID.
     */
    private final ConcurrentHashMap<Long, StateMachine> cache = new ConcurrentHashMap<>();

    /**
     * Get or build a state machine for the given definition ID.
     *
     * @param definitionId workflow definition ID
     * @return the state machine
     */
    public StateMachine getStateMachine(Long definitionId) {
        return cache.computeIfAbsent(definitionId, this::buildStateMachine);
    }

    /**
     * Get or build the active state machine.
     *
     * @return the active state machine, or null if no active definition exists
     */
    public StateMachine getActiveStateMachine() {
        LambdaQueryWrapper<WfDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDefinition::getStatus, 1)
                .orderByDesc(WfDefinition::getVersion)
                .last("LIMIT 1");
        WfDefinition definition = definitionMapper.selectOne(wrapper);
        if (definition == null) {
            log.warn("No active workflow definition found");
            return null;
        }
        return getStateMachine(definition.getId());
    }

    /**
     * Invalidate the cache for a specific definition.
     *
     * @param definitionId workflow definition ID
     */
    public void invalidate(Long definitionId) {
        cache.remove(definitionId);
        log.info("Invalidated state machine cache for definitionId={}", definitionId);
    }

    /**
     * Invalidate the entire cache.
     */
    public void invalidateAll() {
        cache.clear();
        log.info("Invalidated all state machine caches");
    }

    private StateMachine buildStateMachine(Long definitionId) {
        log.info("Building state machine for definitionId={}", definitionId);

        WfDefinition definition = definitionMapper.selectById(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + definitionId);
        }

        LambdaQueryWrapper<WfTransition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTransition::getDefinitionId, definitionId)
                .orderByAsc(WfTransition::getSortOrder);
        List<WfTransition> transitions = transitionMapper.selectList(wrapper);

        return builder.build(definition, transitions);
    }
}
