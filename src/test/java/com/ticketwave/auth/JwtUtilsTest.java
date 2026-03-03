package com.ticketwave.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {
    private JwtUtils jwtUtils;
    private static final String LONG_SECRET = "changeitchangeitchangeitchangeit"; // 32 chars minimum
    private static final long EXPIRATION_MS = 3600000;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(LONG_SECRET, EXPIRATION_MS);
    }

    @Test
    void generateTokenSuccess() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "CUSTOMER");

        String token = jwtUtils.generate(claims, "john");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    void parseTokenSuccess() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "CUSTOMER");
        String subject = "john";

        String token = jwtUtils.generate(claims, subject);
        Jws<Claims> parsed = jwtUtils.parse(token);

        assertNotNull(parsed);
        assertEquals(subject, parsed.getBody().getSubject());
        assertEquals("CUSTOMER", parsed.getBody().get("role"));
    }

    @Test
    void tokenContainsExpiration() {
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtils.generate(claims, "john");
        Jws<Claims> parsed = jwtUtils.parse(token);

        assertNotNull(parsed.getBody().getExpiration());
        assertTrue(parsed.getBody().getExpiration().getTime() > System.currentTimeMillis());
    }

    @Test
    void parseInvalidToken() {
        assertThrows(Exception.class, () -> jwtUtils.parse("invalid.token.here"));
    }

    @Test
    void shortSecretGeneratesKey() {
        // Test with short secret (should log warning and use generated key)
        JwtUtils jwtUtilsShort = new JwtUtils("short", EXPIRATION_MS);
        
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtilsShort.generate(claims, "test");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
}
