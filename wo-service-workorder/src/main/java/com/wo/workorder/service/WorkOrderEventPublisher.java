package com.wo.workorder.service;

public interface WorkOrderEventPublisher {

    /**
     * 发布工单状态变更事件
     */
    void publishStatusChangeEvent(Long workOrderId, String fromStatus, String toStatus);

    /**
     * 发布工单关闭事件
     */
    void publishCloseEvent(Long workOrderId);
}
