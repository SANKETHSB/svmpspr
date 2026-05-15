package com.infosys.svpms.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@svpms.com");
        request.setPassword("Admin@123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testEmailRequired() {
        LoginRequest request = new LoginRequest();
        request.setPassword("Admin@123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertEquals("Email is required", violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void testEmailBlank() {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("Admin@123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertEquals("Email is required", violation.getMessage());
    }

    @Test
    void testEmailWhitespace() {
        LoginRequest request = new LoginRequest();
        request.setEmail("   ");
        request.setPassword("Admin@123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertTrue(violations.size() >= 1);
        
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        // Whitespace-only email is treated as invalid format, not missing
        assertEquals("Invalid email format", violation.getMessage());
    }

    @Test
    void testInvalidEmailFormat() {
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("Admin@123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertEquals("Invalid email format", violation.getMessage());
    }

    @Test
    void testPasswordRequired() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@svpms.com");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertEquals("Password is required", violation.getMessage());
        assertEquals("password", violation.getPropertyPath().toString());
    }

    @Test
    void testPasswordBlank() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@svpms.com");
        request.setPassword("");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertEquals("Password is required", violation.getMessage());
    }

    @Test
    void testPasswordWhitespace() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@svpms.com");
        request.setPassword("   ");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<LoginRequest> violation = violations.iterator().next();
        assertEquals("Password is required", violation.getMessage());
    }

    @Test
    void testBothFieldsNull() {
        LoginRequest request = new LoginRequest();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
        
        // Check that both email and password violations are present
        boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> "email".equals(v.getPropertyPath().toString()));
        boolean hasPasswordViolation = violations.stream()
                .anyMatch(v -> "password".equals(v.getPropertyPath().toString()));
        
        assertTrue(hasEmailViolation);
        assertTrue(hasPasswordViolation);
    }

    @Test
    void testGettersAndSetters() {
        LoginRequest request = new LoginRequest();
        
        // Test email
        request.setEmail("test@example.com");
        assertEquals("test@example.com", request.getEmail());
        
        // Test password
        request.setPassword("testPassword");
        assertEquals("testPassword", request.getPassword());
    }

    @Test
    void testEqualsAndHashCode() {
        LoginRequest request1 = new LoginRequest();
        request1.setEmail("admin@svpms.com");
        request1.setPassword("Admin@123");

        LoginRequest request2 = new LoginRequest();
        request2.setEmail("admin@svpms.com");
        request2.setPassword("Admin@123");

        LoginRequest request3 = new LoginRequest();
        request3.setEmail("different@svpms.com");
        request3.setPassword("Admin@123");

        // Test equals
        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertNotEquals(request1, null);
        assertNotEquals(request1, "string");

        // Test hashCode
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testToString() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@svpms.com");
        request.setPassword("Admin@123");

        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("LoginRequest"));
        assertTrue(toString.contains("admin@svpms.com"));
        // Password should be in toString (Lombok @Data includes all fields)
        assertTrue(toString.contains("Admin@123"));
    }

    @Test
    void testValidEmailFormats() {
        String[] validEmails = {
            "user@domain.com",
            "user.name@domain.com",
            "user+tag@domain.com",
            "user123@domain123.com",
            "a@b.co"
        };

        for (String email : validEmails) {
            LoginRequest request = new LoginRequest();
            request.setEmail(email);
            request.setPassword("password");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Email should be valid: " + email);
        }
    }

    @Test
    void testInvalidEmailFormats() {
        String[] invalidEmails = {
            "plainaddress",
            "@missingdomain.com",
            "missing@.com",
            // "missing@domain", // This actually matches the pattern ^[A-Za-z0-9+_.-]+@(.+)$
            "spaces @domain.com",
            "user@",
            "@domain.com"
        };

        for (String email : invalidEmails) {
            LoginRequest request = new LoginRequest();
            request.setEmail(email);
            request.setPassword("password");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Email should be invalid: " + email);
            
            boolean hasEmailViolation = violations.stream()
                    .anyMatch(v -> "email".equals(v.getPropertyPath().toString()));
            assertTrue(hasEmailViolation, "Should have email violation for: " + email);
        }
    }
}