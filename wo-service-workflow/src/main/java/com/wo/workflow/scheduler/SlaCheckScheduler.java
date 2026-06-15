package com.wo.workflow.scheduler;

import com.wo.workflow.service.SlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for checking SLA breaches.
 * Runs every 60 seconds to detect work orders that have exceeded their SLA deadlines.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaCheckScheduler {

    private final SlaService slaService;

    /**
     * Check for SLA breaches every 60 seconds.
     */
    @Scheduled(fixedDelay = 60000)
    public void checkSlaBreaches() {
        log.debug("Running SLA breach check scheduler");
        try {
            slaService.checkSlaBreaches();
        } catch (Exception e) {
            log.error("Error during SLA breach check", e);
        }
    }
}
