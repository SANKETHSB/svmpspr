package com.infosys.svpms.repository;
import com.infosys.svpms.entity.ComplianceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceDocumentRepository extends JpaRepository<ComplianceDocument, Long> {

    List<ComplianceDocument> findByVendorId(Long vendorId);

    /** AC #8: Latest version only per vendor */
    List<ComplianceDocument> findByVendorIdAndLatestTrue(Long vendorId);

    List<ComplianceDocument> findByVendorIdAndExpiredFalse(Long vendorId);

    /** AC #8: Version history for a specific document type */
    List<ComplianceDocument> findByVendorIdAndDocumentTypeOrderByVersionDesc(Long vendorId, String documentType);

    /** AC #8: Latest version for a specific document type */
    Optional<ComplianceDocument> findByVendorIdAndDocumentTypeAndLatestTrue(Long vendorId, String documentType);

    @Query("SELECT d FROM ComplianceDocument d WHERE d.expiryDate <= :threshold AND d.expired = false AND d.latest = true")
    List<ComplianceDocument> findExpiringBefore(@Param("threshold") LocalDate threshold);

    @Query("SELECT d FROM ComplianceDocument d WHERE d.expiryDate < :today AND d.expired = false AND d.latest = true")
    List<ComplianceDocument> findExpiredDocuments(@Param("today") LocalDate today);

    /** AC #6: Admin compliance summary — count non-compliant vendors */
    @Query("SELECT COUNT(DISTINCT d.vendor.id) FROM ComplianceDocument d WHERE d.expired = true AND d.latest = true")
    long countVendorsWithExpiredDocs();

    /** AC #6: Count documents expiring within N days */
    @Query("SELECT COUNT(d) FROM ComplianceDocument d WHERE d.expiryDate <= :threshold AND d.expired = false AND d.latest = true")
    long countExpiringBefore(@Param("threshold") LocalDate threshold);

    /** AC #6: All latest docs for admin compliance overview */
    @Query("SELECT d FROM ComplianceDocument d WHERE d.latest = true ORDER BY d.expiryDate ASC")
    List<ComplianceDocument> findAllLatestOrderByExpiry();

    /** AC #8: Mark previous versions as not-latest */
    @Modifying
    @Query("UPDATE ComplianceDocument d SET d.latest = false WHERE d.vendor.id = :vendorId AND d.documentType = :docType AND d.latest = true")
    void markPreviousVersionsNotLatest(@Param("vendorId") Long vendorId, @Param("docType") String docType);
}
