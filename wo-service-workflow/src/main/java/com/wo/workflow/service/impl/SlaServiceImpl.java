package com.wo.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wo.workflow.entity.WfSlaRule;
import com.wo.workflow.mapper.SlaRuleMapper;
import com.wo.workflow.service.SlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

        // Update work order SLA deadlines via Feign client
        // This would call wo-service-workorder to update the work order
        // workOrderFeignClient.updateSlaDeadlines(workOrderId, responseDeadline, resolveDeadline);

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

        // Query all active SLA rules
        LambdaQueryWrapper<WfSlaRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfSlaRule::getIsActive, 1);
        // In a real implementation, this would query work orders that have breached their SLA deadlines
        // and send escalation notifications via RocketMQ

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
}
