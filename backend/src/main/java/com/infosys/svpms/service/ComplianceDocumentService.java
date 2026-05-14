package com.infosys.svpms.service;

import com.infosys.svpms.dto.response.ComplianceDocResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface ComplianceDocumentService {

    /** AC #1,#2,#7,#8,#9: Upload with validation, versioning, compliance reset */
    ComplianceDocResponse upload(Long vendorId, String documentType,
                                  String issueDate, String expiryDate,
                                  MultipartFile file, String actorEmail);

    /** AC #1: Latest documents per vendor */
    List<ComplianceDocResponse> getByVendor(Long vendorId);

    /** AC #8: Full version history for a document type */
    List<ComplianceDocResponse> getVersionHistory(Long vendorId, String documentType);

    /** AC #6: Admin compliance summary */
    Map<String, Object> getComplianceSummary();

    /** AC #6: All vendors with compliance status */
    List<Map<String, Object>> getAllVendorComplianceStatus();

    void delete(Long docId, String actorEmail);

    ResponseEntity<Resource> downloadDocument(Long docId);
}
