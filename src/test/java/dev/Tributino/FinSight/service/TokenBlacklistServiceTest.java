package dev.Tributino.FinSight.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService unit tests")
public class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    TokenBlacklistService tokenBlacklistService;

    private static final String PREFIX = "jwt:blacklist:";

    @Test
    @DisplayName("Should successfully revoke an active token by saving it to Redis with TTL")
    void shouldSuccessfullyRevokeActiveToken() {
        String jti = "test-jti-123";
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.revoke(jti, expiresAt);

        verify(stringRedisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).set(
                eq(PREFIX + jti),
                eq("revoked"),
                any(Duration.class)
        );
    }

    @Test
    @DisplayName("Should ignore revocation and not call Redis when token is already expired")
    void shouldNotRevokeExpiredToken() {
        String jti = "test-jti-expired";
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);

        tokenBlacklistService.revoke(jti, expiresAt);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Should return true when token JTI exists in the blacklist")
    void shouldReturnTrueWhenTokenIsRevoked() {
        String jti = "test-jti-revoked";

        when(stringRedisTemplate.hasKey(PREFIX + jti)).thenReturn(true);

        boolean isRevoked = tokenBlacklistService.isRevoked(jti);

        assertTrue(isRevoked);
        verify(stringRedisTemplate, times(1)).hasKey(PREFIX + jti);
    }

    @Test
    @DisplayName("Should return false when token JTI does not exist in the blacklist")
    void shouldReturnFalseWhenTokenIsNotRevoked() {
        String jti = "test-jti-active";

        when(stringRedisTemplate.hasKey(PREFIX + jti)).thenReturn(false);

        boolean isRevoked = tokenBlacklistService.isRevoked(jti);

        assertFalse(isRevoked);
        verify(stringRedisTemplate, times(1)).hasKey(PREFIX + jti);
    }

    @Test
    @DisplayName("Should return false immediately when checking revocation with a null JTI")
    void shouldReturnFalseWhenJtiIsNull() {
        boolean isRevoked = tokenBlacklistService.isRevoked(null);

        assertFalse(isRevoked);
        verify(stringRedisTemplate, never()).hasKey(anyString());
    }
}