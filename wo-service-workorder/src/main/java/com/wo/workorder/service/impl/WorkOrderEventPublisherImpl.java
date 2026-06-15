package com.wo.workorder.service.impl;

import com.wo.workorder.service.WorkOrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderEventPublisherImpl implements WorkOrderEventPublisher {

    private final RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC_STATUS_CHANGE = "wo-status-change";
    private static final String TOPIC_CLOSE = "wo-close";

    @Override
    public void publishStatusChangeEvent(Long workOrderId, String fromStatus, String toStatus) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("workOrderId", workOrderId);
            payload.put("fromStatus", fromStatus);
            payload.put("toStatus", toStatus);
            payload.put("timestamp", System.currentTimeMillis());

            Message<Map<String, Object>> message = MessageBuilder.withPayload(payload)
                    .setHeader("KEYS", String.valueOf(workOrderId))
                    .build();

            rocketMQTemplate.sendOneWay(TOPIC_STATUS_CHANGE, message);
            log.info("状态变更事件已发布, workOrderId={}, from={}, to={}", workOrderId, fromStatus, toStatus);
        } catch (Exception e) {
            log.error("状态变更事件发布失败, workOrderId={}", workOrderId, e);
        }
    }

    @Override
    public void publishCloseEvent(Long workOrderId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("workOrderId", workOrderId);
            payload.put("timestamp", System.currentTimeMillis());

            Message<Map<String, Object>> message = MessageBuilder.withPayload(payload)
                    .setHeader("KEYS", String.valueOf(workOrderId))
                    .build();

            rocketMQTemplate.sendOneWay(TOPIC_CLOSE, message);
            log.info("关闭事件已发布, workOrderId={}", workOrderId);
        } catch (Exception e) {
            log.error("关闭事件发布失败, workOrderId={}", workOrderId, e);
        }
    }
}
