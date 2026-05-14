package com.infosys.svpms.repository;
import com.infosys.svpms.entity.RFQ;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RFQRepository extends JpaRepository<RFQ, Long> {
    Optional<RFQ> findByRfqNumber(String rfqNumber);
    boolean existsByRfqNumber(String rfqNumber);
    List<RFQ> findByStatus(RFQ.RFQStatus status);

    @Query("SELECT r FROM RFQ r WHERE r.status = 'OPEN' AND r.deadline <= :now")
    List<RFQ> findExpiredOpenRFQs(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT r FROM RFQ r JOIN r.vendorInvites vi WHERE vi.vendor.id = :vendorId")
    Page<RFQ> findRFQsForVendor(@Param("vendorId") Long vendorId, Pageable pageable);

    /**
     * US 12 AC #2: Filter RFQs by status (Open, Closed, Awarded, Archived)
     * US 12 AC #4: Multi-criteria filtering supported
     * US 12 AC #5: Results support pagination
     * US 12 AC #6: Sorting (ascending/descending) via Pageable
     * US 12 AC #7: Case-insensitive search
     * US 12 AC #10: Optimized with indexed fields (title, rfq_number, status, deadline)
     * 
     * Search criteria:
     * - RFQ title (partial match, case-insensitive)
     * - RFQ number (partial match, case-insensitive)
     * - Status filter (OPEN, CLOSED, AWARDED, ARCHIVED)
     * - Created by user ID (exact match, optional)
     * - Deadline range (from/to dates, optional)
     * 
     * @param search Search term for title or RFQ number
     * @param status RFQ status filter
     * @param createdById Filter by creator user ID
     * @param deadlineFrom Filter RFQs with deadline after this date
     * @param deadlineTo Filter RFQs with deadline before this date
     * @param pageable Pagination and sorting parameters
     * @return Page of RFQs matching criteria
     */
    @Query("SELECT r FROM RFQ r WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(r.title) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(r.rfqNumber) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:createdById IS NULL OR r.createdBy.id = :createdById) " +
           "AND (:deadlineFrom IS NULL OR r.deadline >= :deadlineFrom) " +
           "AND (:deadlineTo IS NULL OR r.deadline <= :deadlineTo)")
    Page<RFQ> searchRFQs(
        @Param("search") String search, 
        @Param("status") RFQ.RFQStatus status,
        @Param("createdById") Long createdById,
        @Param("deadlineFrom") LocalDateTime deadlineFrom,
        @Param("deadlineTo") LocalDateTime deadlineTo,
        Pageable pageable
    );

    List<RFQ> findByCreatedAtAfter(LocalDateTime date);
    
    long countByStatus(RFQ.RFQStatus status);
}
