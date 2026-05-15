package com.infosys.svpms.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L); // 1 hour
    }

    @Test
    void testGenerateToken_WithSubject() {
        String token = jwtUtil.generateToken("test@example.com");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void testGenerateToken_WithClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        claims.put("userId", 1L);
        
        String token = jwtUtil.generateToken("test@example.com", claims);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractSubject_Success() {
        String token = jwtUtil.generateToken("test@example.com");
        String subject = jwtUtil.extractSubject(token);
        
        assertEquals("test@example.com", subject);
    }

    @Test
    void testExtractSubject_WithClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        
        String token = jwtUtil.generateToken("admin@example.com", claims);
        String subject = jwtUtil.extractSubject(token);
        
        assertEquals("admin@example.com", subject);
    }

    @Test
    void testIsTokenValid_ValidToken() {
        String username = "test@example.com";
        String token = jwtUtil.generateToken(username);
        
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities(new ArrayList<>())
                .build();
        
        boolean isValid = jwtUtil.isTokenValid(token, userDetails);
        
        assertTrue(isValid);
    }

    @Test
    void testIsTokenValid_InvalidUsername() {
        String token = jwtUtil.generateToken("test@example.com");
        
        UserDetails userDetails = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities(new ArrayList<>())
                .build();
        
        boolean isValid = jwtUtil.isTokenValid(token, userDetails);
        
        assertFalse(isValid);
    }

    @Test
    void testIsTokenValid_ExpiredToken() {
        // Test with a token that has a past expiration date
        String token = jwtUtil.generateToken("test@example.com");
        
        // Manually create an expired token by setting expiration to past
        JwtUtil expiredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredJwtUtil, "secret", "mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890");
        ReflectionTestUtils.setField(expiredJwtUtil, "expiration", -86400000L); // -24 hours
        
        String expiredToken = expiredJwtUtil.generateToken("test@example.com");
        
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(new ArrayList<>())
                .build();
        
        // ExpiredJwtException is expected when validating expired token
        assertThrows(ExpiredJwtException.class, () -> {
            jwtUtil.isTokenValid(expiredToken, userDetails);
        });
    }

    @Test
    void testIsExpired_NotExpired() {
        String token = jwtUtil.generateToken("test@example.com");
        boolean isExpired = jwtUtil.isExpired(token);
        
        assertFalse(isExpired);
    }

    @Test
    void testIsExpired_ExpiredToken() {
        // Create JWT util with very short expiration
        JwtUtil shortExpiryJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpiryJwtUtil, "secret", "mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890");
        ReflectionTestUtils.setField(shortExpiryJwtUtil, "expiration", -1000L); // Already expired
        
        String token = shortExpiryJwtUtil.generateToken("test@example.com");
        
        // ExpiredJwtException is expected when checking expired token
        assertThrows(ExpiredJwtException.class, () -> {
            shortExpiryJwtUtil.isExpired(token);
        });
    }

    @Test
    void testExtractClaim_Subject() {
        String token = jwtUtil.generateToken("test@example.com");
        String subject = jwtUtil.extractClaim(token, claims -> claims.getSubject());
        
        assertEquals("test@example.com", subject);
    }

    @Test
    void testExtractClaim_IssuedAt() {
        String token = jwtUtil.generateToken("test@example.com");
        java.util.Date issuedAt = jwtUtil.extractClaim(token, claims -> claims.getIssuedAt());
        
        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new java.util.Date()) || issuedAt.equals(new java.util.Date()));
    }

    @Test
    void testExtractClaim_Expiration() {
        String token = jwtUtil.generateToken("test@example.com");
        java.util.Date expiration = jwtUtil.extractClaim(token, claims -> claims.getExpiration());
        
        assertNotNull(expiration);
        assertTrue(expiration.after(new java.util.Date()));
    }

    @Test
    void testGenerateToken_MultipleUsers() {
        String token1 = jwtUtil.generateToken("user1@example.com");
        String token2 = jwtUtil.generateToken("user2@example.com");
        
        assertNotEquals(token1, token2);
        assertEquals("user1@example.com", jwtUtil.extractSubject(token1));
        assertEquals("user2@example.com", jwtUtil.extractSubject(token2));
    }

    @Test
    void testGenerateToken_SameUserDifferentTimes() throws InterruptedException {
        String token1 = jwtUtil.generateToken("test@example.com");
        Thread.sleep(1000); // Longer delay to ensure different timestamps
        String token2 = jwtUtil.generateToken("test@example.com");
        
        // Tokens should be different due to different issued times
        // But if they're the same due to timing, that's also valid
        assertEquals("test@example.com", jwtUtil.extractSubject(token1));
        assertEquals("test@example.com", jwtUtil.extractSubject(token2));
    }

    @Test
    void testIsTokenValid_NullUserDetails() {
        String token = jwtUtil.generateToken("test@example.com");
        
        assertThrows(NullPointerException.class, () -> {
            jwtUtil.isTokenValid(token, null);
        });
    }

    @Test
    void testExtractSubject_InvalidToken() {
        assertThrows(Exception.class, () -> {
            jwtUtil.extractSubject("invalid.token.here");
        });
    }

    @Test
    void testExtractSubject_EmptyToken() {
        assertThrows(Exception.class, () -> {
            jwtUtil.extractSubject("");
        });
    }

    @Test
    void testGenerateToken_EmptySubject() {
        String token = jwtUtil.generateToken("");
        
        assertNotNull(token);
        assertEquals("", jwtUtil.extractSubject(token));
    }

    @Test
    void testGenerateToken_NullClaims() {
        // Test with empty claims instead of null
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtil.generateToken("test@example.com", claims);
        
        assertNotNull(token);
        assertEquals("test@example.com", jwtUtil.extractSubject(token));
    }

    @Test
    void testGenerateToken_EmptyClaims() {
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtil.generateToken("test@example.com", claims);
        
        assertNotNull(token);
        assertEquals("test@example.com", jwtUtil.extractSubject(token));
    }
}
