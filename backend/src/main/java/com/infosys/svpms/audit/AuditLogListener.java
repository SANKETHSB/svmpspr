package com.infosys.svpms.audit;

import com.infosys.svpms.entity.AuditLog;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * JPA EntityListener that hard-enforces audit log immutability (US 14 #4).
 *
 * Any UPDATE or DELETE attempt issued through the JPA EntityManager
 * triggers an {@link IllegalStateException}. The retention purge is
 * the ONLY allowed deletion path, and it executes via a native query
 * that does not flow through this listener.
 */
public class AuditLogListener {

    @PreUpdate
    public void blockUpdate(AuditLog log) {
        throw new IllegalStateException(
            "Audit logs are immutable. Update of AuditLog id=" + log.getId() + " was blocked.");
    }

    @PreRemove
    public void blockDelete(AuditLog log) {
        throw new IllegalStateException(
            "Audit logs are immutable. Deletion of AuditLog id=" + log.getId()
            + " was blocked. Use the retention purge (native) for compliant cleanup.");
    }
}
