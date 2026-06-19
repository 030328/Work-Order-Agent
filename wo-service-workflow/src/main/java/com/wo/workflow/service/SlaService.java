package com.wo.workflow.service;

import com.wo.workflow.entity.WfSlaRule;

import java.time.LocalDateTime;

/**
 * Service for SLA management.
 */
public interface SlaService {

    /**
     * Assign SLA deadlines to a work order based on its priority.
     *
     * @param workOrderId the work order ID
     * @param priority    priority level (e.g., P1, P2, P3, P4)
     */
    void assignSla(Long workOrderId, String priority);

    /**
     * Check for SLA breaches across all active work orders.
     * Sends escalation notifications via RocketMQ for breached orders.
     */
    void checkSlaBreaches();

    /**
     * Calculate the resolve deadline for a priority without mutating work order data.
     */
    LocalDateTime calculateResolveDeadline(String priority);

    /**
     * Get the active SLA rule for a given priority level.
     *
     * @param priority priority level
     * @return the SLA rule, or null if not found
     */
    WfSlaRule getSlaRule(String priority);
}
