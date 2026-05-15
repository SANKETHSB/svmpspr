package com.infosys.svpms.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRoleRequest() {
        RoleRequest request = new RoleRequest();
        request.setName("CUSTOM_ROLE");
        request.setDescription("A custom role for testing");
        
        RoleRequest.ModulePermissionRequest permission = new RoleRequest.ModulePermissionRequest();
        permission.setModule("USER_MANAGEMENT");
        permission.setCanCreate(true);
        permission.setCanRead(true);
        permission.setCanUpdate(false);
        permission.setCanDelete(false);
        
        request.setPermissions(Arrays.asList(permission));

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNameRequired() {
        RoleRequest request = new RoleRequest();
        request.setDescription("Description without name");

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<RoleRequest> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testNameBlank() {
        RoleRequest request = new RoleRequest();
        request.setName("");
        request.setDescription("Description");

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<RoleRequest> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testNameWhitespace() {
        RoleRequest request = new RoleRequest();
        request.setName("   ");
        request.setDescription("Description");

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<RoleRequest> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testNameTooLong() {
        RoleRequest request = new RoleRequest();
        request.setName("A".repeat(101)); // 101 characters, exceeds max of 100
        request.setDescription("Description");

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<RoleRequest> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testNameMaxLength() {
        RoleRequest request = new RoleRequest();
        request.setName("A".repeat(100)); // Exactly 100 characters, should be valid
        request.setDescription("Description");

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testDescriptionOptional() {
        RoleRequest request = new RoleRequest();
        request.setName("VALID_ROLE");
        // description is null

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testDescriptionTooLong() {
        RoleRequest request = new RoleRequest();
        request.setName("VALID_ROLE");
        request.setDescription("A".repeat(501)); // 501 characters, exceeds max of 500

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        
        ConstraintViolation<RoleRequest> violation = violations.iterator().next();
        assertEquals("description", violation.getPropertyPath().toString());
    }

    @Test
    void testDescriptionMaxLength() {
        RoleRequest request = new RoleRequest();
        request.setName("VALID_ROLE");
        request.setDescription("A".repeat(500)); // Exactly 500 characters, should be valid

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testPermissionsOptional() {
        RoleRequest request = new RoleRequest();
        request.setName("VALID_ROLE");
        request.setDescription("Description");
        // permissions is null

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testEmptyPermissionsList() {
        RoleRequest request = new RoleRequest();
        request.setName("VALID_ROLE");
        request.setDescription("Description");
        request.setPermissions(Arrays.asList()); // empty list

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testModulePermissionValidation() {
        RoleRequest.ModulePermissionRequest permission = new RoleRequest.ModulePermissionRequest();
        permission.setModule("USER_MANAGEMENT");
        permission.setCanCreate(true);
        permission.setCanRead(true);
        permission.setCanUpdate(false);
        permission.setCanDelete(false);

        Set<ConstraintViolation<RoleRequest.ModulePermissionRequest>> violations = 
                validator.validate(permission);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testModulePermissionModuleRequired() {
        RoleRequest.ModulePermissionRequest permission = new RoleRequest.ModulePermissionRequest();
        // module is null
        permission.setCanCreate(true);
        permission.setCanRead(true);

        Set<ConstraintViolation<RoleRequest.ModulePermissionRequest>> violations = 
                validator.validate(permission);
        assertEquals(1, violations.size());
        
        ConstraintViolation<RoleRequest.ModulePermissionRequest> violation = violations.iterator().next();
        assertEquals("module", violation.getPropertyPath().toString());
    }

    @Test
    void testModulePermissionModuleBlank() {
        RoleRequest.ModulePermissionRequest permission = new RoleRequest.ModulePermissionRequest();
        permission.setModule("");
        permission.setCanCreate(true);

        Set<ConstraintViolation<RoleRequest.ModulePermissionRequest>> violations = 
                validator.validate(permission);
        assertEquals(1, violations.size());
        
        ConstraintViolation<RoleRequest.ModulePermissionRequest> violation = violations.iterator().next();
        assertEquals("module", violation.getPropertyPath().toString());
    }

    @Test
    void testNestedPermissionValidation() {
        RoleRequest request = new RoleRequest();
        request.setName("VALID_ROLE");
        
        // Create invalid permission (missing module)
        RoleRequest.ModulePermissionRequest invalidPermission = new RoleRequest.ModulePermissionRequest();
        invalidPermission.setCanCreate(true);
        
        request.setPermissions(Arrays.asList(invalidPermission));

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        // Note: Nested validation is not enabled (no @Valid annotation on permissions list)
        // So this should pass at the RoleRequest level
        assertTrue(violations.isEmpty(), "RoleRequest level validation should pass");
        
        // But if we validate the nested object directly, it should fail
        Set<ConstraintViolation<RoleRequest.ModulePermissionRequest>> nestedViolations = 
            validator.validate(invalidPermission);
        assertTrue(nestedViolations.size() >= 1, "Nested object should have violations for missing module");
    }

    @Test
    void testMultiplePermissions() {
        RoleRequest request = new RoleRequest();
        request.setName("MULTI_PERMISSION_ROLE");
        
        RoleRequest.ModulePermissionRequest permission1 = new RoleRequest.ModulePermissionRequest();
        permission1.setModule("USER_MANAGEMENT");
        permission1.setCanCreate(true);
        permission1.setCanRead(true);
        
        RoleRequest.ModulePermissionRequest permission2 = new RoleRequest.ModulePermissionRequest();
        permission2.setModule("VENDOR_MANAGEMENT");
        permission2.setCanRead(true);
        permission2.setCanUpdate(true);
        
        request.setPermissions(Arrays.asList(permission1, permission2));

        Set<ConstraintViolation<RoleRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        RoleRequest request = new RoleRequest();
        
        // Test name
        request.setName("TEST_ROLE");
        assertEquals("TEST_ROLE", request.getName());
        
        // Test description
        request.setDescription("Test description");
        assertEquals("Test description", request.getDescription());
        
        // Test permissions
        RoleRequest.ModulePermissionRequest permission = new RoleRequest.ModulePermissionRequest();
        permission.setModule("TEST_MODULE");
        List<RoleRequest.ModulePermissionRequest> permissions = Arrays.asList(permission);
        request.setPermissions(permissions);
        assertEquals(permissions, request.getPermissions());
    }

    @Test
    void testModulePermissionGettersAndSetters() {
        RoleRequest.ModulePermissionRequest permission = new RoleRequest.ModulePermissionRequest();
        
        // Test module
        permission.setModule("TEST_MODULE");
        assertEquals("TEST_MODULE", permission.getModule());
        
        // Test permissions
        permission.setCanCreate(true);
        assertTrue(permission.isCanCreate());
        
        permission.setCanRead(false);
        assertFalse(permission.isCanRead());
        
        permission.setCanUpdate(true);
        assertTrue(permission.isCanUpdate());
        
        permission.setCanDelete(false);
        assertFalse(permission.isCanDelete());
    }

    @Test
    void testEqualsAndHashCode() {
        RoleRequest request1 = new RoleRequest();
        request1.setName("TEST_ROLE");
        request1.setDescription("Description");

        RoleRequest request2 = new RoleRequest();
        request2.setName("TEST_ROLE");
        request2.setDescription("Description");

        RoleRequest request3 = new RoleRequest();
        request3.setName("DIFFERENT_ROLE");
        request3.setDescription("Description");

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
    void testModulePermissionEqualsAndHashCode() {
        RoleRequest.ModulePermissionRequest permission1 = new RoleRequest.ModulePermissionRequest();
        permission1.setModule("TEST_MODULE");
        permission1.setCanCreate(true);
        permission1.setCanRead(false);

        RoleRequest.ModulePermissionRequest permission2 = new RoleRequest.ModulePermissionRequest();
        permission2.setModule("TEST_MODULE");
        permission2.setCanCreate(true);
        permission2.setCanRead(false);

        RoleRequest.ModulePermissionRequest permission3 = new RoleRequest.ModulePermissionRequest();
        permission3.setModule("DIFFERENT_MODULE");
        permission3.setCanCreate(true);
        permission3.setCanRead(false);

        // Test equals
        assertEquals(permission1, permission2);
        assertNotEquals(permission1, permission3);

        // Test hashCode
        assertEquals(permission1.hashCode(), permission2.hashCode());
        assertNotEquals(permission1.hashCode(), permission3.hashCode());
    }

    @Test
    void testToString() {
        RoleRequest request = new RoleRequest();
        request.setName("TEST_ROLE");
        request.setDescription("Test description");

        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("RoleRequest"));
        assertTrue(toString.contains("TEST_ROLE"));
        assertTrue(toString.contains("Test description"));
    }

    @Test
    void testModulePermissionToString() {
        RoleRequest.ModulePermissionRequest permission = new RoleRequest.ModulePermissionRequest();
        permission.setModule("TEST_MODULE");
        permission.setCanCreate(true);
        permission.setCanRead(false);

        String toString = permission.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ModulePermissionRequest"));
        assertTrue(toString.contains("TEST_MODULE"));
        assertTrue(toString.contains("true"));
        assertTrue(toString.contains("false"));
    }

    @Test
    void testPermissionDefaults() {
        RoleRequest.ModulePermissionRequest permission = new RoleRequest.ModulePermissionRequest();
        
        // Boolean fields should default to false
        assertFalse(permission.isCanCreate());
        assertFalse(permission.isCanRead());
        assertFalse(permission.isCanUpdate());
        assertFalse(permission.isCanDelete());
    }

    @Test
    void testAllPermissionCombinations() {
        // Test all possible boolean combinations for permissions
        boolean[] values = {true, false};
        
        for (boolean create : values) {
            for (boolean read : values) {
                for (boolean update : values) {
                    for (boolean delete : values) {
                        RoleRequest.ModulePermissionRequest permission = new RoleRequest.ModulePermissionRequest();
                        permission.setModule("TEST_MODULE");
                        permission.setCanCreate(create);
                        permission.setCanRead(read);
                        permission.setCanUpdate(update);
                        permission.setCanDelete(delete);

                        Set<ConstraintViolation<RoleRequest.ModulePermissionRequest>> violations = 
                                validator.validate(permission);
                        assertTrue(violations.isEmpty(), 
                                String.format("Permission combination should be valid: C=%s, R=%s, U=%s, D=%s", 
                                        create, read, update, delete));
                    }
                }
            }
        }
    }
}