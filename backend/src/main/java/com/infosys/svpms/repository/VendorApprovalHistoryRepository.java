package com.infosys.svpms.repository;

import com.infosys.svpms.entity.VendorApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VendorApprovalHistoryRepository extends JpaRepository<VendorApprovalHistory, Long> {
    List<VendorApprovalHistory> findByVendorIdOrderByActionTimestampDesc(Long vendorId);
}
