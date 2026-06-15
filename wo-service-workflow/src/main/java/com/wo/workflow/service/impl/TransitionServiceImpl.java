package com.wo.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.workflow.entity.WfDefinition;
import com.wo.workflow.entity.WfTransition;
import com.wo.workflow.mapper.TransitionMapper;
import com.wo.workflow.mapper.WorkflowDefinitionMapper;
import com.wo.workflow.service.TransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of TransitionService for managing workflow definitions and transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransitionServiceImpl implements TransitionService {

    private final WorkflowDefinitionMapper definitionMapper;
    private final TransitionMapper transitionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDefinition(WfDefinition definition) {
        log.info("Creating workflow definition: {}", definition.getName());
        definitionMapper.insert(definition);
    }

    @Override
    public WfDefinition getActiveDefinition() {
        LambdaQueryWrapper<WfDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDefinition::getStatus, 1)
                .orderByDesc(WfDefinition::getVersion)
                .last("LIMIT 1");
        return definitionMapper.selectOne(wrapper);
    }

    @Override
    public List<WfTransition> getTransitions(Long definitionId, String fromState) {
        LambdaQueryWrapper<WfTransition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTransition::getDefinitionId, definitionId);
        if (fromState != null && !fromState.isEmpty()) {
            wrapper.eq(WfTransition::getFromState, fromState);
        }
        wrapper.orderByAsc(WfTransition::getSortOrder);
        return transitionMapper.selectList(wrapper);
    }
}
