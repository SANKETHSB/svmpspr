package com.infosys.svpms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Report produced by {@code AuditService#verifyIntegrity()} (US 14 #11).
 *
 * The audit_logs table forms a hash chain. Walking the chain in id-order
 * lets us detect any tampering — including row deletions, oldValue/newValue
 * edits, or actor reassignments — because each record's recordHash depends
 * on the previous record's hash.
 *
 * If {@code intact == true}, no divergence was detected.
 * Otherwise {@code firstBrokenId} identifies the first row whose stored
 * hash does not match the recomputed hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditIntegrityReport {
    private long totalRecords;
    private long verifiedRecords;
    private boolean intact;
    private Long firstBrokenId;
    private String message;
}
