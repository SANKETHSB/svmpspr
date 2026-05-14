package com.infosys.svpms.audit;

import com.infosys.svpms.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * US 14 #9 — audit log retention scheduler.
 *
 * Runs once a day at 03:00 server time and purges audit rows older
 * than {@code app.audit.retention-days}. The purge is itself recorded
 * by an AUDIT_RETENTION_PURGE entry (with SYSTEM as actor) so that
 * the action is fully traceable.
 *
 * Configure via application.properties:
 *   app.audit.retention-days=365     (default)
 *   app.audit.retention-cron=0 0 3 * * *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditRetentionScheduler {

    private final AuditService auditService;

    @Value("${app.audit.retention-days:365}")
    private int retentionDays;

    @Scheduled(cron = "${app.audit.retention-cron:0 0 3 * * *}")
    public void runRetention() {
        try {
            int purged = auditService.applyRetention(retentionDays);
            log.info("Audit retention completed: {} rows purged (retention={} days)", purged, retentionDays);
        } catch (Exception e) {
            log.error("Audit retention failed: {}", e.getMessage(), e);
        }
    }
}
