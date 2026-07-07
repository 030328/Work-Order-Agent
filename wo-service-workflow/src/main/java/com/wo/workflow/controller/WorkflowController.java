package com.wo.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.workflow.entity.WfDefinition;
import com.wo.workflow.entity.WfTransition;
import com.wo.workflow.mapper.WorkflowDefinitionMapper;
import com.wo.workflow.service.TransitionService;
import com.wo.common.security.InternalServiceAuth;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing workflow definitions.
 */
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final TransitionService transitionService;
    private final WorkflowDefinitionMapper definitionMapper;
    private final InternalServiceAuth internalServiceAuth;

    /**
     * Create a new workflow definition.
     */
    @PostMapping("/definitions")
    public ResponseEntity<WfDefinition> createDefinition(@RequestBody WfDefinition definition,
                                                         HttpServletRequest request) {
        internalServiceAuth.require(request);
        transitionService.createDefinition(definition);
        return ResponseEntity.ok(definition);
    }

    /**
     * List all workflow definitions.
     */
    @GetMapping("/definitions")
    public ResponseEntity<List<WfDefinition>> listDefinitions(HttpServletRequest request) {
        internalServiceAuth.require(request);
        LambdaQueryWrapper<WfDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WfDefinition::getCreatedAt);
        List<WfDefinition> definitions = definitionMapper.selectList(wrapper);
        return ResponseEntity.ok(definitions);
    }

    /**
     * Get a workflow definition by ID.
     */
    @GetMapping("/definitions/{id}")
    public ResponseEntity<WfDefinition> getDefinition(@PathVariable Long id,
                                                      HttpServletRequest request) {
        internalServiceAuth.require(request);
        WfDefinition definition = definitionMapper.selectById(id);
        if (definition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(definition);
    }

    /**
     * Get all transitions for a workflow definition.
     */
    @GetMapping("/definitions/{id}/transitions")
    public ResponseEntity<List<WfTransition>> getTransitions(@PathVariable Long id,
                                                             HttpServletRequest request) {
        internalServiceAuth.require(request);
        List<WfTransition> transitions = transitionService.getTransitions(id, null);
        return ResponseEntity.ok(transitions);
    }
}
