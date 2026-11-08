package dev.Tributino.FinSight.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.Tributino.FinSight.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService unit tests")
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private User user;

    private final String secret = "senha_secreta_test";
    private final String issuer = "FinSight API";
    private final String userEmail = "user@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "secret", secret);
    }

    @Test
    @DisplayName("Should successfully generate a JWT token when user is valid")
    void shouldSuccessfullyGenerateTokenWhenUserIsValid() {
        when(user.getEmail()).thenReturn(userEmail);

        String token = tokenService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        DecodedJWT decodedJWT = tokenService.decodeToken(token);
        assertNotNull(decodedJWT);
        assertEquals(userEmail, decodedJWT.getSubject());
        assertEquals(issuer, decodedJWT.getIssuer());
        assertNotNull(decodedJWT.getId());
    }

    @Test
    @DisplayName("Should throw RuntimeException when JWT token generation fails due to creation exception")
    void shouldThrowRuntimeExceptionWhenTokenGenerationFails() {
        ReflectionTestUtils.setField(tokenService, "secret", (String) null);

        Exception exception = assertThrows(Exception.class, () -> {
            tokenService.generateToken(user);
        });

        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully decode a valid JWT token and return DecodedJWT")
    void shouldSuccessfullyDecodeValidToken() {
        when(user.getEmail()).thenReturn(userEmail);
        String token = tokenService.generateToken(user);

        DecodedJWT decodedJWT = tokenService.decodeToken(token);

        assertNotNull(decodedJWT);
        assertEquals(userEmail, decodedJWT.getSubject());
        assertEquals(issuer, decodedJWT.getIssuer());
    }

    @Test
    @DisplayName("Should return null when JWT token is invalid or verification fails")
    void shouldReturnNullWhenTokenIsInvalid() {
        String invalidToken = "invalid.jwt.token";

        DecodedJWT decodedJWT = tokenService.decodeToken(invalidToken);

        assertNull(decodedJWT);
    }

    @Test
    @DisplayName("Should return null when JWT token is expired")
    void shouldReturnNullWhenTokenIsExpired() {
        Algorithm algorithm = Algorithm.HMAC256(secret);

        String expiredToken = JWT.create()
                .withIssuer(issuer)
                .withSubject(userEmail)
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .sign(algorithm);

        DecodedJWT decodedJWT = tokenService.decodeToken(expiredToken);

        assertNull(decodedJWT);
    }

    @Test
    @DisplayName("Should return null when JWT token has a different issuer")
    void shouldReturnNullWhenTokenHasDifferentIssuer() {
        Algorithm algorithm = Algorithm.HMAC256(secret);

        String tokenWithDifferentIssuer = JWT.create()
                .withIssuer("Other API")
                .withSubject(userEmail)
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .sign(algorithm);

        DecodedJWT decodedJWT = tokenService.decodeToken(tokenWithDifferentIssuer);

        assertNull(decodedJWT);
    }
}