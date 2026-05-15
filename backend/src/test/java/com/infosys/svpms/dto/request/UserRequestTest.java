package com.infosys.svpms.dto.request;

import com.infosys.svpms.entity.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidUserRequest() {
        UserRequest request = new UserRequest();
        request.setName("John Doe");
        request.setEmail("john.doe@svpms.com");
        request.setPassword("SecurePass123");
        request.setRole(User.Role.PROCUREMENT_MANAGER);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNameRequired() {
        UserRequest request = new UserRequest();
        request.setEmail("john.doe@svpms.com");
        request.setPassword("SecurePass123");
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<UserRequest> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testNameBlank() {
        UserRequest request = new UserRequest();
        request.setName("");
        request.setEmail("john.doe@svpms.com");
        request.setPassword("SecurePass123");
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<UserRequest> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testNameWhitespace() {
        UserRequest request = new UserRequest();
        request.setName("   ");
        request.setEmail("john.doe@svpms.com");
        request.setPassword("SecurePass123");
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<UserRequest> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testNameTooLong() {
        UserRequest request = new UserRequest();
        request.setName("A".repeat(101)); // 101 characters, exceeds max of 100
        request.setEmail("john.doe@svpms.com");
        request.setPassword("SecurePass123");
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<UserRequest> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testNameMaxLength() {
        UserRequest request = new UserRequest();
        request.setName("A".repeat(100)); // Exactly 100 characters, should be valid
        request.setEmail("john.doe@svpms.com");
        request.setPassword("SecurePass123");
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testEmailRequired() {
        UserRequest request = new UserRequest();
        request.setName("John Doe");
        request.setPassword("SecurePass123");
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<UserRequest> violation = violations.iterator().next();
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void testEmailBlank() {
        UserRequest request = new UserRequest();
        request.setName("John Doe");
        request.setEmail("");
        request.setPassword("SecurePass123");
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<UserRequest> violation = violations.iterator().next();
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void testInvalidEmailFormat() {
        UserRequest request = new UserRequest();
        request.setName("John Doe");
        request.setEmail("invalid-email");
        request.setPassword("SecurePass123");
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<UserRequest> violation = violations.iterator().next();
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void testPasswordTooShort() {
        UserRequest request = new UserRequest();
        request.setName("John Doe");
        request.setEmail("john.doe@svpms.com");
        request.setPassword("1234567"); // 7 characters, less than minimum 8
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<UserRequest> violation = violations.iterator().next();
        assertEquals("password", violation.getPropertyPath().toString());
        assertEquals("Password must be at least 8 characters", violation.getMessage());
    }

    @Test
    void testPasswordMinLength() {
        UserRequest request = new UserRequest();
        request.setName("John Doe");
        request.setEmail("john.doe@svpms.com");
        request.setPassword("12345678"); // Exactly 8 characters, should be valid
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testPasswordCanBeNull() {
        // Password is not marked as @NotNull, so null should be allowed
        UserRequest request = new UserRequest();
        request.setName("John Doe");
        request.setEmail("john.doe@svpms.com");
        request.setPassword(null);
        request.setRole(User.Role.ADMIN);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testRoleRequired() {
        UserRequest request = new UserRequest();
        request.setName("John Doe");
        request.setEmail("john.doe@svpms.com");
        request.setPassword("SecurePass123");
        // role is null

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<UserRequest> violation = violations.iterator().next();
        assertEquals("role", violation.getPropertyPath().toString());
    }

    @Test
    void testAllRoleValues() {
        // Test that all User.Role enum values are accepted
        User.Role[] roles = User.Role.values();
        
        for (User.Role role : roles) {
            UserRequest request = new UserRequest();
            request.setName("John Doe");
            request.setEmail("john.doe@svpms.com");
            request.setPassword("SecurePass123");
            request.setRole(role);

            Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Role should be valid: " + role);
        }
    }

    @Test
    void testMultipleValidationErrors() {
        UserRequest request = new UserRequest();
        // All fields invalid
        request.setName(""); // blank
        request.setEmail("invalid"); // invalid format
        request.setPassword("123"); // too short
        // role is null

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
        assertEquals(4, violations.size());
        
        // Check that all expected violations are present
        boolean hasNameViolation = violations.stream()
                .anyMatch(v -> "name".equals(v.getPropertyPath().toString()));
        boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> "email".equals(v.getPropertyPath().toString()));
        boolean hasPasswordViolation = violations.stream()
                .anyMatch(v -> "password".equals(v.getPropertyPath().toString()));
        boolean hasRoleViolation = violations.stream()
                .anyMatch(v -> "role".equals(v.getPropertyPath().toString()));
        
        assertTrue(hasNameViolation);
        assertTrue(hasEmailViolation);
        assertTrue(hasPasswordViolation);
        assertTrue(hasRoleViolation);
    }

    @Test
    void testGettersAndSetters() {
        UserRequest request = new UserRequest();
        
        // Test name
        request.setName("Test Name");
        assertEquals("Test Name", request.getName());
        
        // Test email
        request.setEmail("test@example.com");
        assertEquals("test@example.com", request.getEmail());
        
        // Test password
        request.setPassword("testPassword");
        assertEquals("testPassword", request.getPassword());
        
        // Test role
        request.setRole(User.Role.ADMIN);
        assertEquals(User.Role.ADMIN, request.getRole());
    }

    @Test
    void testEqualsAndHashCode() {
        UserRequest request1 = new UserRequest();
        request1.setName("John Doe");
        request1.setEmail("john@svpms.com");
        request1.setPassword("password123");
        request1.setRole(User.Role.ADMIN);

        UserRequest request2 = new UserRequest();
        request2.setName("John Doe");
        request2.setEmail("john@svpms.com");
        request2.setPassword("password123");
        request2.setRole(User.Role.ADMIN);

        UserRequest request3 = new UserRequest();
        request3.setName("Jane Doe");
        request3.setEmail("jane@svpms.com");
        request3.setPassword("password123");
        request3.setRole(User.Role.ADMIN);

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
        UserRequest request = new UserRequest();
        request.setName("John Doe");
        request.setEmail("john@svpms.com");
        request.setPassword("password123");
        request.setRole(User.Role.ADMIN);

        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("UserRequest"));
        assertTrue(toString.contains("John Doe"));
        assertTrue(toString.contains("john@svpms.com"));
        assertTrue(toString.contains("ADMIN"));
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
            UserRequest request = new UserRequest();
            request.setName("Test User");
            request.setEmail(email);
            request.setPassword("password123");
            request.setRole(User.Role.ADMIN);

            Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
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
            UserRequest request = new UserRequest();
            request.setName("Test User");
            request.setEmail(email);
            request.setPassword("password123");
            request.setRole(User.Role.ADMIN);

            Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Email should be invalid: " + email);
            
            boolean hasEmailViolation = violations.stream()
                    .anyMatch(v -> "email".equals(v.getPropertyPath().toString()));
            assertTrue(hasEmailViolation, "Should have email violation for: " + email);
        }
    }

    @Test
    void testPublicFieldAccess() {
        // Test that fields are public (as declared in the DTO)
        UserRequest request = new UserRequest();
        
        // Direct field access should work
        request.name = "Direct Access";
        request.email = "direct@access.com";
        request.password = "directpass";
        request.role = User.Role.COMPLIANCE_OFFICER;
        
        assertEquals("Direct Access", request.name);
        assertEquals("direct@access.com", request.email);
        assertEquals("directpass", request.password);
        assertEquals(User.Role.COMPLIANCE_OFFICER, request.role);
    }
}