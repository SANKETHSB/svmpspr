package com.infosys.svpms.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void testUserBuilder() {
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password123")
                .role(User.Role.ADMIN)
                .active(true)
                .failedLoginCount(0)
                .locked(false)
                .build();

        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals(User.Role.ADMIN, user.getRole());
        assertTrue(user.isActive());
        assertEquals(0, user.getFailedLoginCount());
        assertFalse(user.isLocked());
    }

    @Test
    void testUserGettersAndSetters() {
        User user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("pass123");
        user.setRole(User.Role.PROCUREMENT_MANAGER);
        user.setCustomRoleName("Custom");
        user.setActive(true);
        user.setFailedLoginCount(2);
        user.setLocked(true);
        
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        user.setLockedUntil(now.plusHours(1));

        assertEquals(1L, user.getId());
        assertEquals("John Doe", user.getName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("pass123", user.getPassword());
        assertEquals(User.Role.PROCUREMENT_MANAGER, user.getRole());
        assertEquals("Custom", user.getCustomRoleName());
        assertTrue(user.isActive());
        assertEquals(2, user.getFailedLoginCount());
        assertTrue(user.isLocked());
        assertEquals(now, user.getLastLoginAt());
        assertEquals(now.plusHours(1), user.getLockedUntil());
    }

    @Test
    void testUserRoleEnum() {
        assertEquals(3, User.Role.values().length);
        assertEquals(User.Role.ADMIN, User.Role.valueOf("ADMIN"));
        assertEquals(User.Role.PROCUREMENT_MANAGER, User.Role.valueOf("PROCUREMENT_MANAGER"));
        assertEquals(User.Role.COMPLIANCE_OFFICER, User.Role.valueOf("COMPLIANCE_OFFICER"));
    }

    @Test
    void testUserDefaults() {
        User user = User.builder().build();
        assertTrue(user.isActive());
        assertEquals(0, user.getFailedLoginCount());
        assertFalse(user.isLocked());
    }

    @Test
    void testUserAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(1L, "Test", "test@test.com", "pass", User.Role.ADMIN, 
                "Custom", true, 0, false, now, now.plusHours(1), now, now);
        
        assertEquals(1L, user.getId());
        assertEquals("Test", user.getName());
        assertEquals("test@test.com", user.getEmail());
    }

    @Test
    void testUserNoArgsConstructor() {
        User user = new User();
        assertNotNull(user);
        assertNull(user.getId());
        assertNull(user.getName());
    }
}
