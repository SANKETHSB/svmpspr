package com.infosys.svpms.repository;
import com.infosys.svpms.entity.PurchaseOrder;
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
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
    boolean existsByRfqId(Long rfqId);
    List<PurchaseOrder> findByVendorId(Long vendorId);

    /**
     * US 12 AC #3: Filter POs by vendor, date range, and status
     * US 12 AC #4: Multi-criteria filtering supported
     * US 12 AC #5: Results support pagination
     * US 12 AC #6: Sorting (ascending/descending) via Pageable
     * US 12 AC #7: Case-insensitive search
     * US 12 AC #10: Optimized with indexed fields (po_number, vendor_id, status, generated_at)
     * 
     * Search criteria:
     * - PO number (partial match, case-insensitive)
     * - Vendor ID (exact match, optional)
     * - Status filter (GENERATED, ISSUED, RECEIVED, CLOSED, CANCELLED)
     * - Date range (from/to dates for generated_at)
     * - RFQ number (partial match, case-insensitive)
     * 
     * @param search Search term for PO number or RFQ number
     * @param vendorId Vendor ID filter
     * @param status PO status filter
     * @param dateFrom Filter POs generated after this date
     * @param dateTo Filter POs generated before this date
     * @param pageable Pagination and sorting parameters
     * @return Page of POs matching criteria
     */
    @Query("SELECT p FROM PurchaseOrder p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.poNumber) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(p.rfq.rfqNumber) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:vendorId IS NULL OR p.vendor.id = :vendorId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:dateFrom IS NULL OR p.generatedAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR p.generatedAt <= :dateTo)")
    Page<PurchaseOrder> filterPOs(
        @Param("search") String search,
        @Param("vendorId") Long vendorId, 
        @Param("status") PurchaseOrder.POStatus status,
        @Param("dateFrom") LocalDateTime dateFrom,
        @Param("dateTo") LocalDateTime dateTo,
        Pageable pageable
    );
    
    // US 10: Analytics queries
    List<PurchaseOrder> findByVendorIdAndGeneratedAtBetween(Long vendorId, LocalDateTime start, LocalDateTime end);
}
