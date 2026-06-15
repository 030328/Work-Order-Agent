package com.wo.workorder.mq;

import com.wo.workorder.entity.WoWorkOrder;
import com.wo.workorder.mapper.WorkOrderMapper;
import com.wo.workorder.service.WorkOrderSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "wo-status-change",
        consumerGroup = "workorder-status-change-consumer"
)
public class WorkOrderEventConsumer implements RocketMQListener<Map<String, Object>> {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderSearchService workOrderSearchService;

    @Override
    public void onMessage(Map<String, Object> message) {
        Long workOrderId = Long.valueOf(String.valueOf(message.get("workOrderId")));
        String fromStatus = (String) message.get("fromStatus");
        String toStatus = (String) message.get("toStatus");

        log.info("收到工单状态变更事件, workOrderId={}, from={}, to={}", workOrderId, fromStatus, toStatus);

        try {
            // 更新ES索引
            WoWorkOrder workOrder = workOrderMapper.selectById(workOrderId);
            if (workOrder != null) {
                workOrderSearchService.indexWorkOrder(workOrder);
                log.info("ES索引更新成功, workOrderId={}", workOrderId);
            }
        } catch (Exception e) {
            log.error("处理工单状态变更事件失败, workOrderId={}", workOrderId, e);
        }
    }
}
