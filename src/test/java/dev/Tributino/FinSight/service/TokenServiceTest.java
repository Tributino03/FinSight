package dev.Tributino.FinSight.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.Tributino.FinSight.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService Unit Tests")
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

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("should successfully generate a JWT token when user is valid")
        void shouldSuccessfullyGenerateTokenWhenUserIsValid() {
            when(user.getEmail()).thenReturn(userEmail);

            String token = tokenService.generateToken(user);

            assertThat(token).isNotNull().isNotEmpty();

            DecodedJWT decodedJWT = tokenService.decodeToken(token);
            assertThat(decodedJWT).isNotNull();
            assertThat(decodedJWT.getSubject()).isEqualTo(userEmail);
            assertThat(decodedJWT.getIssuer()).isEqualTo(issuer);
            assertThat(decodedJWT.getId()).isNotNull();
        }

        @Test
        @DisplayName("should throw Exception when JWT token generation fails due to missing secret")
        void shouldThrowExceptionWhenTokenGenerationFails() {
            ReflectionTestUtils.setField(tokenService, "secret", (String) null);

            assertThatThrownBy(() -> tokenService.generateToken(user))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("decodeToken")
    class DecodeToken {

        @Test
        @DisplayName("should successfully decode a valid JWT token and return DecodedJWT")
        void shouldSuccessfullyDecodeValidToken() {
            when(user.getEmail()).thenReturn(userEmail);
            String token = tokenService.generateToken(user);

            DecodedJWT decodedJWT = tokenService.decodeToken(token);

            assertThat(decodedJWT).isNotNull();
            assertThat(decodedJWT.getSubject()).isEqualTo(userEmail);
            assertThat(decodedJWT.getIssuer()).isEqualTo(issuer);
        }

        @Test
        @DisplayName("should return null when JWT token is invalid or verification fails")
        void shouldReturnNullWhenTokenIsInvalid() {
            String invalidToken = "invalid.jwt.token";

            DecodedJWT decodedJWT = tokenService.decodeToken(invalidToken);

            assertThat(decodedJWT).isNull();
        }

        @Test
        @DisplayName("should return null when JWT token is expired")
        void shouldReturnNullWhenTokenIsExpired() {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            String expiredToken = JWT.create()
                    .withIssuer(issuer)
                    .withSubject(userEmail)
                    .withJWTId(UUID.randomUUID().toString())
                    .withExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .sign(algorithm);

            DecodedJWT decodedJWT = tokenService.decodeToken(expiredToken);

            assertThat(decodedJWT).isNull();
        }

        @Test
        @DisplayName("should return null when JWT token has a different issuer")
        void shouldReturnNullWhenTokenHasDifferentIssuer() {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            String tokenWithDifferentIssuer = JWT.create()
                    .withIssuer("Other API")
                    .withSubject(userEmail)
                    .withJWTId(UUID.randomUUID().toString())
                    .withExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                    .sign(algorithm);

            DecodedJWT decodedJWT = tokenService.decodeToken(tokenWithDifferentIssuer);

            assertThat(decodedJWT).isNull();
        }
    }
}