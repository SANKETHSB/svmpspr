package com.infosys.svpms.repository;
import com.infosys.svpms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actorId IS NULL OR a.actorId = :actorId) " +
           "AND (:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%',:action,'%'))) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:from IS NULL OR a.timestamp >= :from) " +
           "AND (:to IS NULL OR a.timestamp <= :to) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> filterLogs(@Param("actorId") Long actorId,
                               @Param("action") String action,
                               @Param("entityType") String entityType,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               Pageable pageable);

    /** Most recent record in id-order — used to seed the hash chain link (US 14 #11). */
    Optional<AuditLog> findTopByOrderByIdDesc();

    /** Streams records in id-order for hash-chain verification (US 14 #11). */
    @Query("SELECT a FROM AuditLog a ORDER BY a.id ASC")
    java.util.List<AuditLog> findAllOrderedById();

    /**
     * Retention purge (US 14 #9). Native query intentionally bypasses the
     * {@code AuditLogListener} so that legitimate, scheduled cleanup is
     * possible. The cleanup itself is audited by the caller.
     */
    @Modifying
    @Query(value = "DELETE FROM audit_logs WHERE timestamp < :cutoff", nativeQuery = true)
    int purgeOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
