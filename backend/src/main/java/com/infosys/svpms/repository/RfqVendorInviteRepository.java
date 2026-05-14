package com.infosys.svpms.repository;
import com.infosys.svpms.entity.RfqVendorInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RfqVendorInviteRepository extends JpaRepository<RfqVendorInvite, Long> {
    List<RfqVendorInvite> findByRfqId(Long rfqId);
    List<RfqVendorInvite> findByVendorId(Long vendorId);
    Optional<RfqVendorInvite> findByRfqIdAndVendorId(Long rfqId, Long vendorId);
    boolean existsByRfqIdAndVendorId(Long rfqId, Long vendorId);
}
