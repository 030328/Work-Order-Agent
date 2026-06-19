package com.wo.workflow.service.impl;

import com.wo.api.client.WorkOrderClient;
import com.wo.common.result.R;
import com.wo.workflow.entity.WfSlaRule;
import com.wo.workflow.mapper.SlaRuleMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlaServiceImplTest {

    private final SlaRuleMapper slaRuleMapper = mock(SlaRuleMapper.class);
    private final RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
    private final WorkOrderClient workOrderClient = mock(WorkOrderClient.class);
    private final SlaServiceImpl slaService = new SlaServiceImpl(slaRuleMapper, rocketMQTemplate, workOrderClient);

    @Test
    void calculateResolveDeadlineShouldUseActiveRuleResolveHours() {
        when(slaRuleMapper.selectOne(any())).thenReturn(rule("HIGH", 4, 24));

        LocalDateTime before = LocalDateTime.now().plusHours(23);
        LocalDateTime deadline = slaService.calculateResolveDeadline("HIGH");
        LocalDateTime after = LocalDateTime.now().plusHours(25);

        assertThat(deadline).isAfter(before);
        assertThat(deadline).isBefore(after);
    }

    @Test
    void assignSlaShouldUpdateWorkOrderDeadlineAndSendNotification() {
        when(slaRuleMapper.selectOne(any())).thenReturn(rule("HIGH", 4, 24));
        when(workOrderClient.updateSlaDeadline(eq(1001L), anyString())).thenReturn(R.ok());

        slaService.assignSla(1001L, "HIGH");

        verify(workOrderClient).updateSlaDeadline(eq(1001L), anyString());
        verify(rocketMQTemplate).send(eq("sla-assign-topic"), any(Message.class));
    }

    @Test
    void assignSlaShouldSkipMutationWhenRuleIsMissing() {
        when(slaRuleMapper.selectOne(any())).thenReturn(null);

        slaService.assignSla(1001L, "UNKNOWN");

        verify(workOrderClient, never()).updateSlaDeadline(any(), anyString());
        verify(rocketMQTemplate, never()).send(anyString(), any(Message.class));
    }

    @Test
    void checkSlaBreachesShouldMarkBreachedOrdersAndSendEvent() {
        when(workOrderClient.listSlaBreachedWorkOrderIds(anyString())).thenReturn(R.ok(List.of(1001L, 1002L)));
        when(workOrderClient.markSlaBreached(1001L)).thenReturn(R.ok());
        when(workOrderClient.markSlaBreached(1002L)).thenReturn(R.ok());

        slaService.checkSlaBreaches();

        verify(workOrderClient).markSlaBreached(1001L);
        verify(workOrderClient).markSlaBreached(1002L);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate, org.mockito.Mockito.times(2)).send(topicCaptor.capture(), any(Message.class));
        assertThat(topicCaptor.getAllValues()).containsOnly("sla-breach-topic");
    }

    private WfSlaRule rule(String priority, int responseHours, int resolveHours) {
        WfSlaRule rule = new WfSlaRule();
        rule.setPriority(priority);
        rule.setResponseHours(responseHours);
        rule.setResolveHours(resolveHours);
        rule.setIsActive(1);
        return rule;
    }
}
