package com.wo.workorder.service.impl;

import com.wo.workorder.service.WorkOrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderEventPublisherImpl implements WorkOrderEventPublisher {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${workorder.event.mq-enabled:true}")
    private boolean mqEnabled;

    private static final String TOPIC_STATUS_CHANGE = "wo-status-change";
    private static final String TOPIC_CLOSE = "wo-close";

    @Override
    public void publishStatusChangeEvent(Long workOrderId, String fromStatus, String toStatus) {
        if (!mqEnabled) {
            log.debug("MQ事件发布已关闭, workOrderId={}, from={}, to={}", workOrderId, fromStatus, toStatus);
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("workOrderId", workOrderId);
            payload.put("fromStatus", fromStatus);
            payload.put("toStatus", toStatus);
            payload.put("timestamp", System.currentTimeMillis());

            Message<Map<String, Object>> message = MessageBuilder.withPayload(payload)
                    .setHeader("KEYS", String.valueOf(workOrderId))
                    .build();

            publishAfterCommit(() -> {
                rocketMQTemplate.sendOneWay(TOPIC_STATUS_CHANGE, message);
                log.info("状态变更事件已发布, workOrderId={}, from={}, to={}", workOrderId, fromStatus, toStatus);
            });
        } catch (Exception e) {
            log.warn("状态变更事件发布失败，主流程已继续，workOrderId={}, reason={}",
                    workOrderId, e.getMessage());
        }
    }

    @Override
    public void publishCloseEvent(Long workOrderId) {
        if (!mqEnabled) {
            log.debug("MQ关闭事件发布已关闭, workOrderId={}", workOrderId);
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("workOrderId", workOrderId);
            payload.put("timestamp", System.currentTimeMillis());

            Message<Map<String, Object>> message = MessageBuilder.withPayload(payload)
                    .setHeader("KEYS", String.valueOf(workOrderId))
                    .build();

            publishAfterCommit(() -> {
                rocketMQTemplate.sendOneWay(TOPIC_CLOSE, message);
                log.info("关闭事件已发布, workOrderId={}", workOrderId);
            });
        } catch (Exception e) {
            log.warn("关闭事件发布失败，主流程已继续，workOrderId={}, reason={}",
                    workOrderId, e.getMessage());
        }
    }

    private void publishAfterCommit(Runnable publisher) {
        Runnable safePublisher = () -> {
            try {
                publisher.run();
            } catch (Exception e) {
                log.warn("事务提交后发布MQ事件失败，主流程已完成，reason={}", e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safePublisher.run();
                }
            });
            return;
        }
        safePublisher.run();
    }
}
