package com.infosys.svpms.service;

import com.infosys.svpms.dto.AuditIntegrityReport;
import com.infosys.svpms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface AuditService {
    void log(Long actorId, String actorType, String actorName, String action,
             String entityType, Long entityId, String description);
    void log(Long actorId, String actorType, String actorName, String action,
             String entityType, Long entityId, String oldValue, String newValue, String description);
    Page<AuditLog> getLogs(Long actorId, String action, String entityType,
                           LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** Export with optional date range (US 14 #6). */
    byte[] exportCsv(String entityType, LocalDateTime from, LocalDateTime to);

    /** Walks the hash chain and reports the first divergence (US 14 #11). */
    AuditIntegrityReport verifyIntegrity();

    /** Purges audit rows older than {@code retentionDays} via native query (US 14 #9). */
    int applyRetention(int retentionDays);
}
