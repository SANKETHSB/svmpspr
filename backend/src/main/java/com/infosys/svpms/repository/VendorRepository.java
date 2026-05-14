package com.infosys.svpms.repository;

import com.infosys.svpms.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByEmail(String email);
    Optional<Vendor> findByGstNumber(String gstNumber);
    Optional<Vendor> findByRegistrationId(String registrationId);
    boolean existsByEmail(String email);
    boolean existsByGstNumber(String gstNumber);
    boolean existsByRegistrationId(String registrationId);
    List<Vendor> findByStatus(Vendor.VendorStatus status);

    @Query("SELECT v FROM Vendor v WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(v.companyName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(v.gstNumber) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(v.registrationId) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:status IS NULL OR v.status = :status) " +
           "AND (:compliant IS NULL OR v.compliant = :compliant)")
    Page<Vendor> searchVendors(
        @Param("search") String search,
        @Param("status") Vendor.VendorStatus status,
        @Param("compliant") Boolean compliant,
        Pageable pageable
    );

    /** AC #6: Count compliant vendors for compliance summary */
    long countByCompliantTrue();
}
