package com.infosys.svpms.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditIntegrityReportTest {

    @Test
    void testBuilderPattern() {
        AuditIntegrityReport report = AuditIntegrityReport.builder()
                .totalRecords(1000L)
                .verifiedRecords(1000L)
                .intact(true)
                .firstBrokenId(null)
                .message("All audit records verified successfully")
                .build();

        assertEquals(1000L, report.getTotalRecords());
        assertEquals(1000L, report.getVerifiedRecords());
        assertTrue(report.isIntact());
        assertNull(report.getFirstBrokenId());
        assertEquals("All audit records verified successfully", report.getMessage());
    }

    @Test
    void testIntactAuditReport() {
        AuditIntegrityReport report = AuditIntegrityReport.builder()
                .totalRecords(500L)
                .verifiedRecords(500L)
                .intact(true)
                .message("Audit chain is intact")
                .build();

        assertEquals(500L, report.getTotalRecords());
        assertEquals(500L, report.getVerifiedRecords());
        assertTrue(report.isIntact());
        assertNull(report.getFirstBrokenId());
        assertEquals("Audit chain is intact", report.getMessage());
    }

    @Test
    void testBrokenAuditReport() {
        AuditIntegrityReport report = AuditIntegrityReport.builder()
                .totalRecords(1000L)
                .verifiedRecords(750L)
                .intact(false)
                .firstBrokenId(751L)
                .message("Hash chain broken at record 751")
                .build();

        assertEquals(1000L, report.getTotalRecords());
        assertEquals(750L, report.getVerifiedRecords());
        assertFalse(report.isIntact());
        assertEquals(Long.valueOf(751L), report.getFirstBrokenId());
        assertEquals("Hash chain broken at record 751", report.getMessage());
    }

    @Test
    void testNoArgsConstructor() {
        AuditIntegrityReport report = new AuditIntegrityReport();
        
        assertEquals(0L, report.getTotalRecords());
        assertEquals(0L, report.getVerifiedRecords());
        assertFalse(report.isIntact());
        assertNull(report.getFirstBrokenId());
        assertNull(report.getMessage());
    }

    @Test
    void testAllArgsConstructor() {
        AuditIntegrityReport report = new AuditIntegrityReport(
                2000L, 1500L, false, 1501L, "Integrity check failed"
        );

        assertEquals(2000L, report.getTotalRecords());
        assertEquals(1500L, report.getVerifiedRecords());
        assertFalse(report.isIntact());
        assertEquals(Long.valueOf(1501L), report.getFirstBrokenId());
        assertEquals("Integrity check failed", report.getMessage());
    }

    @Test
    void testGettersAndSetters() {
        AuditIntegrityReport report = new AuditIntegrityReport();
        
        // Test totalRecords
        report.setTotalRecords(100L);
        assertEquals(100L, report.getTotalRecords());
        
        // Test verifiedRecords
        report.setVerifiedRecords(95L);
        assertEquals(95L, report.getVerifiedRecords());
        
        // Test intact
        report.setIntact(true);
        assertTrue(report.isIntact());
        
        report.setIntact(false);
        assertFalse(report.isIntact());
        
        // Test firstBrokenId
        report.setFirstBrokenId(96L);
        assertEquals(Long.valueOf(96L), report.getFirstBrokenId());
        
        report.setFirstBrokenId(null);
        assertNull(report.getFirstBrokenId());
        
        // Test message
        report.setMessage("Test message");
        assertEquals("Test message", report.getMessage());
    }

    @Test
    void testEqualsAndHashCode() {
        AuditIntegrityReport report1 = AuditIntegrityReport.builder()
                .totalRecords(100L)
                .verifiedRecords(100L)
                .intact(true)
                .message("Success")
                .build();

        AuditIntegrityReport report2 = AuditIntegrityReport.builder()
                .totalRecords(100L)
                .verifiedRecords(100L)
                .intact(true)
                .message("Success")
                .build();

        AuditIntegrityReport report3 = AuditIntegrityReport.builder()
                .totalRecords(200L)
                .verifiedRecords(100L)
                .intact(false)
                .firstBrokenId(101L)
                .message("Failed")
                .build();

        // Test equals
        assertEquals(report1, report2);
        assertNotEquals(report1, report3);
        assertNotEquals(report1, null);
        assertNotEquals(report1, "string");

        // Test hashCode
        assertEquals(report1.hashCode(), report2.hashCode());
        assertNotEquals(report1.hashCode(), report3.hashCode());
    }

    @Test
    void testToString() {
        AuditIntegrityReport report = AuditIntegrityReport.builder()
                .totalRecords(1000L)
                .verifiedRecords(950L)
                .intact(false)
                .firstBrokenId(951L)
                .message("Hash mismatch detected")
                .build();

        String toString = report.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("AuditIntegrityReport"));
        assertTrue(toString.contains("1000"));
        assertTrue(toString.contains("950"));
        assertTrue(toString.contains("false"));
        assertTrue(toString.contains("951"));
        assertTrue(toString.contains("Hash mismatch detected"));
    }

    @Test
    void testEmptyAuditReport() {
        AuditIntegrityReport report = AuditIntegrityReport.builder()
                .totalRecords(0L)
                .verifiedRecords(0L)
                .intact(true)
                .message("No audit records to verify")
                .build();

        assertEquals(0L, report.getTotalRecords());
        assertEquals(0L, report.getVerifiedRecords());
        assertTrue(report.isIntact());
        assertNull(report.getFirstBrokenId());
        assertEquals("No audit records to verify", report.getMessage());
    }

    @Test
    void testPartialVerificationScenarios() {
        // Scenario 1: All records verified successfully
        AuditIntegrityReport successReport = AuditIntegrityReport.builder()
                .totalRecords(1000L)
                .verifiedRecords(1000L)
                .intact(true)
                .build();
        
        assertTrue(successReport.isIntact());
        assertEquals(successReport.getTotalRecords(), successReport.getVerifiedRecords());
        
        // Scenario 2: Verification stopped at broken record
        AuditIntegrityReport brokenReport = AuditIntegrityReport.builder()
                .totalRecords(1000L)
                .verifiedRecords(500L)
                .intact(false)
                .firstBrokenId(501L)
                .build();
        
        assertFalse(brokenReport.isIntact());
        assertTrue(brokenReport.getVerifiedRecords() < brokenReport.getTotalRecords());
        assertNotNull(brokenReport.getFirstBrokenId());
        
        // Scenario 3: First record is broken
        AuditIntegrityReport firstBrokenReport = AuditIntegrityReport.builder()
                .totalRecords(1000L)
                .verifiedRecords(0L)
                .intact(false)
                .firstBrokenId(1L)
                .build();
        
        assertFalse(firstBrokenReport.isIntact());
        assertEquals(0L, firstBrokenReport.getVerifiedRecords());
        assertEquals(Long.valueOf(1L), firstBrokenReport.getFirstBrokenId());
    }

    @Test
    void testLargeNumbers() {
        long maxLong = Long.MAX_VALUE;
        
        AuditIntegrityReport report = AuditIntegrityReport.builder()
                .totalRecords(maxLong)
                .verifiedRecords(maxLong - 1)
                .intact(false)
                .firstBrokenId(maxLong)
                .message("Large dataset test")
                .build();

        assertEquals(maxLong, report.getTotalRecords());
        assertEquals(maxLong - 1, report.getVerifiedRecords());
        assertFalse(report.isIntact());
        assertEquals(Long.valueOf(maxLong), report.getFirstBrokenId());
    }

    @Test
    void testNullValues() {
        AuditIntegrityReport report = AuditIntegrityReport.builder()
                .totalRecords(100L)
                .verifiedRecords(100L)
                .intact(true)
                .firstBrokenId(null)
                .message(null)
                .build();

        assertEquals(100L, report.getTotalRecords());
        assertEquals(100L, report.getVerifiedRecords());
        assertTrue(report.isIntact());
        assertNull(report.getFirstBrokenId());
        assertNull(report.getMessage());
    }

    @Test
    void testBuilderDefaults() {
        AuditIntegrityReport report = AuditIntegrityReport.builder().build();
        
        assertEquals(0L, report.getTotalRecords());
        assertEquals(0L, report.getVerifiedRecords());
        assertFalse(report.isIntact());
        assertNull(report.getFirstBrokenId());
        assertNull(report.getMessage());
    }

    @Test
    void testIntegrityLogic() {
        // When intact is true, firstBrokenId should typically be null
        AuditIntegrityReport intactReport = AuditIntegrityReport.builder()
                .totalRecords(100L)
                .verifiedRecords(100L)
                .intact(true)
                .firstBrokenId(null)
                .build();
        
        assertTrue(intactReport.isIntact());
        assertNull(intactReport.getFirstBrokenId());
        
        // When intact is false, firstBrokenId should typically be set
        AuditIntegrityReport brokenReport = AuditIntegrityReport.builder()
                .totalRecords(100L)
                .verifiedRecords(50L)
                .intact(false)
                .firstBrokenId(51L)
                .build();
        
        assertFalse(brokenReport.isIntact());
        assertNotNull(brokenReport.getFirstBrokenId());
        assertTrue(brokenReport.getFirstBrokenId() > brokenReport.getVerifiedRecords());
    }

    @Test
    void testMessageVariations() {
        String[] messages = {
            "All audit records verified successfully",
            "Hash chain broken at record 123",
            "Integrity check completed with warnings",
            "No audit records found",
            "Verification interrupted",
            ""
        };
        
        for (String message : messages) {
            AuditIntegrityReport report = AuditIntegrityReport.builder()
                    .message(message)
                    .build();
            
            assertEquals(message, report.getMessage());
        }
    }
}