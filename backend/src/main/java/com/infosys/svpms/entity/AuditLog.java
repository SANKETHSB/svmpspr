package com.infosys.svpms.entity;

import com.infosys.svpms.audit.AuditLogListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

/**
 * Immutable audit record for SVPMS (US 14).
 *
 * Immutability is enforced at three layers:
 *   1. {@code @Immutable} (Hibernate) blocks UPDATE through the JPA session.
 *   2. {@link AuditLogListener} throws on @PreUpdate / @PreRemove.
 *   3. Retention purges use a native query that bypasses the listener
 *      (and self-audits the purge with a SYSTEM actor).
 *
 * Tamper-evidence is provided by a SHA-256 hash chain:
 *   recordHash = SHA-256(id|actorId|action|entityType|entityId|oldValue|newValue|timestamp|prevHash)
 * If any field of any past record is mutated, the chain breaks and
 * /audit-logs/integrity will report the first divergence.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_actor", columnList = "actorId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity", columnList = "entityType"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Immutable
@EntityListeners(AuditLogListener.class)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long actorId;

    @Column(nullable = false, updatable = false, length = 20)
    private String actorType; // USER, VENDOR, SYSTEM, ANONYMOUS

    @Column(updatable = false)
    private String actorName;

    @Column(nullable = false, updatable = false, length = 100)
    private String action;

    @Column(nullable = false, updatable = false, length = 100)
    private String entityType;

    @Column(nullable = false, updatable = false)
    private Long entityId;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @Column(length = 1000, updatable = false)
    private String description;

    @Column(length = 45, updatable = false)
    private String ipAddress;

    /** True when oldValue/newValue were sanitized to mask sensitive fields. */
    @Column(updatable = false, nullable = false)
    @Builder.Default
    private boolean sensitiveDataMasked = false;

    /** SHA-256 hash of the previous audit record (links the chain). */
    @Column(length = 64, updatable = false)
    private String prevHash;

    /** SHA-256 hash of THIS record's content. Recomputed by /integrity to detect tampering. */
    @Column(length = 64, updatable = false)
    private String recordHash;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;
}
