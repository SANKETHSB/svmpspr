package com.infosys.svpms.service.impl;

import com.infosys.svpms.audit.AuditSanitizer;
import com.infosys.svpms.dto.AuditIntegrityReport;
import com.infosys.svpms.entity.AuditLog;
import com.infosys.svpms.repository.AuditLogRepository;
import com.infosys.svpms.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementation of the audit service. Implements US 14 acceptance criteria
 * #3 (old/new), #4 (immutable — via listener), #6 (export with date filter),
 * #9 (retention), #10 (sanitize sensitive data), #11 (hash-chain integrity).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository repo;

    @Override
    public void log(Long actorId, String actorType, String actorName, String action,
                    String entityType, Long entityId, String description) {
        log(actorId, actorType, actorName, action, entityType, entityId, null, null, description);
    }

    /**
     * Synchronous + transactional so the hash chain remains consistent.
     * (Async writes would race on prevHash lookup.)
     */
    @Override
    @Transactional
    public void log(Long actorId, String actorType, String actorName, String action,
                    String entityType, Long entityId, String oldValue, String newValue, String description) {
        try {
            // US 14 #10 — mask any sensitive data before persistence.
            boolean sensitive = AuditSanitizer.containsSensitive(oldValue) || AuditSanitizer.containsSensitive(newValue);
            String safeOld = AuditSanitizer.mask(oldValue);
            String safeNew = AuditSanitizer.mask(newValue);

            // Capture caller IP if we're inside a request context.
            String ip = currentRequestIp();

            // US 14 #11 — link to the previous record's hash.
            String prevHash = repo.findTopByOrderByIdDesc().map(AuditLog::getRecordHash).orElse("GENESIS");

            AuditLog entry = AuditLog.builder()
                .actorId(actorId == null ? 0L : actorId)
                .actorType(actorType == null ? "SYSTEM" : actorType)
                .actorName(actorName)
                .action(action)
                .entityType(entityType == null ? "System" : entityType)
                .entityId(entityId == null ? 0L : entityId)
                .oldValue(safeOld)
                .newValue(safeNew)
                .description(description)
                .ipAddress(ip)
                .sensitiveDataMasked(sensitive)
                .prevHash(prevHash)
                .build();

            // Compute hash AFTER timestamp is set by Hibernate. Trick: stamp it
            // ourselves so the chain remains deterministic without a second flush.
            entry.setTimestamp(LocalDateTime.now());
            entry.setRecordHash(computeHash(entry));
            repo.save(entry);
        } catch (Exception e) {
            // Audit failure must never break the business operation.
            log.error("Audit log failed (action={}, entity={}): {}", action, entityType, e.getMessage());
        }
    }

    @Override
    public Page<AuditLog> getLogs(Long actorId, String action, String entityType,
                                   LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return repo.filterLogs(actorId, action, entityType, from, to, pageable);
    }

    /** US 14 #6 — export with optional date filter. */
    @Override
    public byte[] exportCsv(String entityType, LocalDateTime from, LocalDateTime to) {
        List<AuditLog> logs = repo.filterLogs(null, null, entityType, from, to, Pageable.unpaged()).getContent();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter pw = new PrintWriter(baos)) {
            pw.println("ID,Actor,ActorType,Action,Entity,EntityId,OldValue,NewValue,Description,IP,Sensitive,Timestamp,RecordHash");
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            logs.forEach(l -> pw.printf("%d,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s%n",
                l.getId(),
                csv(l.getActorName()),
                l.getActorType(),
                l.getAction(),
                l.getEntityType(),
                l.getEntityId(),
                csv(l.getOldValue()),
                csv(l.getNewValue()),
                csv(l.getDescription()),
                l.getIpAddress() == null ? "" : l.getIpAddress(),
                l.isSensitiveDataMasked() ? "YES" : "NO",
                l.getTimestamp() == null ? "" : l.getTimestamp().format(fmt),
                l.getRecordHash() == null ? "" : l.getRecordHash()));
            pw.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Audit CSV export failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    /** US 14 #11 — hash-chain integrity verification. */
    @Override
    public AuditIntegrityReport verifyIntegrity() {
        List<AuditLog> all = repo.findAllOrderedById();
        String expectedPrev = "GENESIS";
        long verified = 0;
        for (AuditLog row : all) {
            if (!expectedPrev.equals(row.getPrevHash() == null ? "" : row.getPrevHash())) {
                return AuditIntegrityReport.builder()
                    .totalRecords(all.size())
                    .verifiedRecords(verified)
                    .intact(false)
                    .firstBrokenId(row.getId())
                    .message("prevHash mismatch at id=" + row.getId() + " — chain broken or row deleted")
                    .build();
            }
            String recomputed = computeHash(row);
            if (!recomputed.equals(row.getRecordHash())) {
                return AuditIntegrityReport.builder()
                    .totalRecords(all.size())
                    .verifiedRecords(verified)
                    .intact(false)
                    .firstBrokenId(row.getId())
                    .message("recordHash mismatch at id=" + row.getId() + " — content tampered")
                    .build();
            }
            expectedPrev = row.getRecordHash();
            verified++;
        }
        return AuditIntegrityReport.builder()
            .totalRecords(all.size())
            .verifiedRecords(verified)
            .intact(true)
            .firstBrokenId(null)
            .message("All " + verified + " audit records are intact.")
            .build();
    }

    /** US 14 #9 — retention purge. Self-audited with a SYSTEM actor. */
    @Override
    @Transactional
    public int applyRetention(int retentionDays) {
        if (retentionDays <= 0) {
            log.warn("Audit retention skipped: retentionDays={}", retentionDays);
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int purged = repo.purgeOlderThan(cutoff);
        // Self-audit the purge so the action itself is traceable.
        log(0L, "SYSTEM", "RetentionScheduler",
            "AUDIT_RETENTION_PURGE", "AuditLog", 0L, null, null,
            "Purged " + purged + " audit rows older than " + cutoff + " (retention=" + retentionDays + " days)");
        return purged;
    }

    // ---------- internals ----------

    /**
     * Deterministic SHA-256 over the immutable fields of the record + prevHash.
     * Using a pipe separator keeps the input unambiguous since none of the
     * fields are allowed to contain a literal '|' that we generate ourselves.
     */
    private String computeHash(AuditLog l) {
        String input = String.join("|",
            String.valueOf(l.getActorId()),
            nz(l.getActorType()),
            nz(l.getAction()),
            nz(l.getEntityType()),
            String.valueOf(l.getEntityId()),
            nz(l.getOldValue()),
            nz(l.getNewValue()),
            nz(l.getDescription()),
            l.getTimestamp() == null ? "" : l.getTimestamp().toString(),
            nz(l.getPrevHash()));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.error("SHA-256 unavailable: {}", e.getMessage());
            return "";
        }
    }

    private String csv(String s) {
        if (s == null) return "";
        return s.replace(",", " ").replace("\n", " ").replace("\r", " ");
    }

    private String nz(String s) { return s == null ? "" : s; }

    private String currentRequestIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String fwd = req.getHeader("X-Forwarded-For");
            if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
