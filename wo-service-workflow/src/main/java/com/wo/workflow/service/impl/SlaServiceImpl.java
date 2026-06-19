package com.wo.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.api.client.WorkOrderClient;
import com.wo.common.result.R;
import com.wo.workflow.entity.WfSlaRule;
import com.wo.workflow.mapper.SlaRuleMapper;
import com.wo.workflow.service.SlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of SLA service.
 * Handles SLA assignment, breach checking, and escalation notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private final SlaRuleMapper slaRuleMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final WorkOrderClient workOrderClient;

    @Override
    public void assignSla(Long workOrderId, String priority) {
        WfSlaRule rule = getSlaRule(priority);
        if (rule == null) {
            log.warn("No SLA rule found for priority: {}", priority);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime responseDeadline = now.plusHours(rule.getResponseHours());
        LocalDateTime resolveDeadline = now.plusHours(rule.getResolveHours());

        log.info("Assigned SLA to workOrderId={}, priority={}, responseDeadline={}, resolveDeadline={}",
                workOrderId, priority, responseDeadline, resolveDeadline);

        R<Void> updateResult = workOrderClient.updateSlaDeadline(workOrderId, resolveDeadline.toString());
        if (updateResult == null || updateResult.getCode() != 0) {
            log.warn("Failed to update work order SLA deadline, workOrderId={}, message={}",
                    workOrderId, updateResult != null ? updateResult.getMessage() : "empty response");
            return;
        }

        // Send notification about SLA assignment
        try {
            String topic = "sla-assign-topic";
            String payload = String.format("{\"workOrderId\":%d,\"priority\":\"%s\",\"responseDeadline\":\"%s\",\"resolveDeadline\":\"%s\"}",
                    workOrderId, priority, responseDeadline, resolveDeadline);
            rocketMQTemplate.send(topic, MessageBuilder.withPayload(payload).build());
            log.info("Sent SLA assignment notification for workOrderId={}", workOrderId);
        } catch (Exception e) {
            log.error("Failed to send SLA assignment notification for workOrderId={}", workOrderId, e);
        }
    }

    @Override
    public void checkSlaBreaches() {
        log.debug("Checking SLA breaches...");
        LocalDateTime now = LocalDateTime.now();

        R<List<Long>> breachedResult = workOrderClient.listSlaBreachedWorkOrderIds(now.toString());
        if (breachedResult == null || breachedResult.getCode() != 0 || breachedResult.getData() == null) {
            log.warn("Failed to query SLA breached work orders: {}",
                    breachedResult != null ? breachedResult.getMessage() : "empty response");
            return;
        }

        for (Long workOrderId : breachedResult.getData()) {
            try {
                R<Void> markResult = workOrderClient.markSlaBreached(workOrderId);
                if (markResult != null && markResult.getCode() == 0) {
                    String topic = "sla-breach-topic";
                    String payload = String.format("{\"workOrderId\":%d,\"breachedAt\":\"%s\"}", workOrderId, now);
                    rocketMQTemplate.send(topic, MessageBuilder.withPayload(payload).build());
                    log.info("SLA breach handled for workOrderId={}", workOrderId);
                } else {
                    log.warn("Failed to mark SLA breach, workOrderId={}, message={}",
                            workOrderId, markResult != null ? markResult.getMessage() : "empty response");
                }
            } catch (Exception e) {
                log.error("Failed to handle SLA breach, workOrderId={}", workOrderId, e);
            }
        }

        log.debug("SLA breach check completed at {}", now);
    }

    @Override
    public WfSlaRule getSlaRule(String priority) {
        if (priority == null || priority.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<WfSlaRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfSlaRule::getPriority, priority)
                .eq(WfSlaRule::getIsActive, 1)
                .last("LIMIT 1");
        return slaRuleMapper.selectOne(wrapper);
    }

    @Override
    public LocalDateTime calculateResolveDeadline(String priority) {
        WfSlaRule rule = getSlaRule(priority);
        if (rule == null) {
            return null;
        }
        return LocalDateTime.now().plusHours(rule.getResolveHours());
    }
}
