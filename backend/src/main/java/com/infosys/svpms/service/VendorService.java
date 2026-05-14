package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.VendorRegisterRequest;
import com.infosys.svpms.dto.response.VendorApprovalHistoryResponse;
import com.infosys.svpms.dto.response.VendorResponse;
import com.infosys.svpms.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VendorService {
    VendorResponse register(VendorRegisterRequest req);
    VendorResponse approve(Long id, String actorEmail);
    VendorResponse reject(Long id, String reason, String actorEmail);
    VendorResponse suspend(Long id, String reason, String actorEmail);
    VendorResponse getById(Long id);
    /**
     * US 12 AC #1: Search vendors by name, GST, registration ID
     * US 12 AC #4: Multi-criteria filtering
     * US 12 AC #5: Pagination support
     * US 12 AC #6: Sorting support
     * 
     * @param search Search term for company name, GST, or registration ID
     * @param status Vendor status filter
     * @param compliant Compliance status filter
     * @param pageable Pagination and sorting parameters
     * @return Page of vendors matching criteria
     */
    Page<VendorResponse> getAll(String search, Vendor.VendorStatus status, Boolean compliant, Pageable pageable);
    List<VendorResponse> getApproved();
    VendorResponse getMyProfile(String email);
    Vendor findByEmail(String email);
    List<VendorApprovalHistoryResponse> getApprovalHistory(Long vendorId);
}
