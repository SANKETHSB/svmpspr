package com.infosys.svpms.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class QuotationEntityTest {

    @Test
    void testQuotationBuilder() {
        Quotation quotation = Quotation.builder()
                .totalAmount(BigDecimal.valueOf(10000))
                .taxPercentage(18.0)
                .currency("INR")
                .deliveryDays(30)
                .status(Quotation.QuotationStatus.SUBMITTED)
                .build();

        assertEquals(BigDecimal.valueOf(10000), quotation.getTotalAmount());
        assertEquals(18.0, quotation.getTaxPercentage());
        assertEquals("INR", quotation.getCurrency());
        assertEquals(30, quotation.getDeliveryDays());
        assertEquals(Quotation.QuotationStatus.SUBMITTED, quotation.getStatus());
    }

    @Test
    void testQuotationGettersAndSetters() {
        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setTotalAmount(BigDecimal.valueOf(5000));
        quotation.setTaxPercentage(10.0);
        quotation.setCurrency("USD");
        quotation.setDeliveryDays(15);
        quotation.setStatus(Quotation.QuotationStatus.AWARDED);
        quotation.setWeightedScore(85.0);

        assertEquals(1L, quotation.getId());
        assertEquals(BigDecimal.valueOf(5000), quotation.getTotalAmount());
        assertEquals(Quotation.QuotationStatus.AWARDED, quotation.getStatus());
        assertEquals(85.0, quotation.getWeightedScore());
    }

    @Test
    void testQuotationStatusEnum() {
        assertEquals(4, Quotation.QuotationStatus.values().length);
        assertEquals(Quotation.QuotationStatus.SUBMITTED, Quotation.QuotationStatus.valueOf("SUBMITTED"));
        assertEquals(Quotation.QuotationStatus.UNDER_EVALUATION, Quotation.QuotationStatus.valueOf("UNDER_EVALUATION"));
        assertEquals(Quotation.QuotationStatus.AWARDED, Quotation.QuotationStatus.valueOf("AWARDED"));
        assertEquals(Quotation.QuotationStatus.REJECTED, Quotation.QuotationStatus.valueOf("REJECTED"));
    }
}
