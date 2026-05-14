package com.infosys.svpms.repository;

import com.infosys.svpms.entity.QuotationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuotationDocumentRepository extends JpaRepository<QuotationDocument, Long> {
    List<QuotationDocument> findByQuotationId(Long quotationId);
    long countByQuotationId(Long quotationId);
}
