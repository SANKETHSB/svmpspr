package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.QuotationEvaluationRequest;
import com.infosys.svpms.dto.request.QuotationRequest;
import com.infosys.svpms.dto.response.QuotationResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface QuotationService {
    QuotationResponse submit(QuotationRequest req, String vendorEmail);
    QuotationResponse resubmit(Long id, QuotationRequest req, String vendorEmail);
    QuotationResponse getById(Long id);
    List<QuotationResponse> getByRfq(Long rfqId);
    List<QuotationResponse> getByVendor(Long vendorId);
    List<QuotationResponse> compareByRfq(Long rfqId);
    QuotationResponse evaluate(Long id, QuotationEvaluationRequest req, String actorEmail);
    void uploadDocument(Long quotationId, MultipartFile file, String vendorEmail);
    List<Map<String, Object>> getDocuments(Long quotationId);
    void rejectLateSubmissions();
    byte[] exportComparisonToPdf(Long rfqId);
    byte[] downloadDocument(Long documentId, String actorEmail);
    QuotationResponse finalizeSubmission(Long quotationId, String vendorEmail);
}
