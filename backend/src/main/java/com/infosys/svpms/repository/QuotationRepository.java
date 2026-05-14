package com.infosys.svpms.repository;
import com.infosys.svpms.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByRfqId(Long rfqId);
    List<Quotation> findByVendorId(Long vendorId);
    Optional<Quotation> findByRfqIdAndVendorId(Long rfqId, Long vendorId);
    boolean existsByRfqIdAndVendorId(Long rfqId, Long vendorId);

    @Query("SELECT q FROM Quotation q WHERE q.rfq.id = :rfqId ORDER BY q.totalAmount ASC")
    List<Quotation> findByRfqIdOrderByAmount(@Param("rfqId") Long rfqId);
    
    // US 10: Analytics queries
    List<Quotation> findByVendorIdAndSubmittedAtBetween(Long vendorId, LocalDateTime start, LocalDateTime end);
}
