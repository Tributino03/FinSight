package dev.Tributino.FinSight.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import dev.Tributino.FinSight.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private TokenBlacklistService blacklistService;

    @Mock
    private Authentication authentication;

    @Mock
    private User user;

    @Mock
    private DecodedJWT decodedJWT;

    @InjectMocks
    private AuthService authService;

    private final String email = "user@example.com";
    private final String password = "password123";
    private final String generatedToken = "mocked.jwt.token";
    private final String tokenJti = "test-jti-uuid";
    private final Instant expiresAt = Instant.now().plusSeconds(3600);

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should successfully authenticate user and return a JWT token on login")
        void shouldSuccessfullyLoginAndReturnToken() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(user);
            when(tokenService.generateToken(user)).thenReturn(generatedToken);

            String token = authService.login(email, password);

            assertThat(token).isNotNull().isEqualTo(generatedToken);
            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenService, times(1)).generateToken(user);
        }

        @Test
        @DisplayName("should throw BadCredentialsException when login fails due to invalid credentials")
        void shouldThrowExceptionWhenLoginFails() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(email, password))
                    .isInstanceOf(BadCredentialsException.class);

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenService, never()).generateToken(any());
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("should successfully decode token and add JTI to blacklist on logout")
        void shouldSuccessfullyLogoutAndRevokeToken() {
            when(tokenService.decodeToken(generatedToken)).thenReturn(decodedJWT);
            when(decodedJWT.getId()).thenReturn(tokenJti);
            when(decodedJWT.getExpiresAtAsInstant()).thenReturn(expiresAt);

            authService.logout(generatedToken);

            verify(tokenService, times(1)).decodeToken(generatedToken);
            verify(blacklistService, times(1)).revoke(tokenJti, expiresAt);
        }

        @Test
        @DisplayName("should ignore logout and not blacklist when token is invalid or null")
        void shouldIgnoreLogoutWhenTokenIsInvalid() {
            when(tokenService.decodeToken(generatedToken)).thenReturn(null);

            authService.logout(generatedToken);

            verify(tokenService, times(1)).decodeToken(generatedToken);
            verify(blacklistService, never()).revoke(any(), any());
        }

        @Test
        @DisplayName("should ignore logout and not blacklist when decoded token has a null JTI")
        void shouldIgnoreLogoutWhenTokenJtiIsNull() {
            when(tokenService.decodeToken(generatedToken)).thenReturn(decodedJWT);
            when(decodedJWT.getId()).thenReturn(null);

            authService.logout(generatedToken);

            verify(tokenService, times(1)).decodeToken(generatedToken);
            verify(decodedJWT, never()).getExpiresAtAsInstant();
            verify(blacklistService, never()).revoke(any(), any());
        }
    }
}