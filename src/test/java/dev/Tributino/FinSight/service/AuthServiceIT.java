package dev.Tributino.FinSight.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import dev.Tributino.FinSight.config.TestcontainersConfig;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@Transactional
class AuthServiceIT {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenBlacklistService blacklistService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        String encodedPassword = passwordEncoder.encode("123456");

        user = new User("Gustavo", "test@gmail.com", encodedPassword);
        userRepository.save(user);
    }

    @Nested
    @DisplayName("Login Method Tests")
    class LoginTests {

        @Test
        @DisplayName("Should authenticate successfully when credentials are valid")
        void shouldLoginSuccessfullyWhenCredentialsAreValid() {
            String token = authService.login(user.getEmail(), "123456");

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void shouldThrowExceptionWhenPasswordIsInvalid() {
            assertThatThrownBy(() -> authService.login(
                    user.getEmail(),
                    "wrong-password"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Usuário inexistente ou senha inválida");
        }

        @Test
        @DisplayName("Should throw exception when email is not registered")
        void shouldThrowExceptionWhenEmailNotFound() {
            assertThatThrownBy(() -> authService.login(
                    "wrong-email@gmail.com",
                    "123456"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Usuário inexistente ou senha inválida");
        }
    }

    @Nested
    @DisplayName("Logout Method Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should add token to blacklist successfully when token is valid")
        void shouldAddTokenToBlacklistWhenTokenIsValid() {
            String token = authService.login(user.getEmail(), "123456");

            DecodedJWT jwt = tokenService.decodeToken(token);
            String jti = jwt.getId();

            assertThat(blacklistService.isRevoked(jti)).isFalse();

            authService.logout(token);

            assertThat(blacklistService.isRevoked(jti)).isTrue();
        }

        @Test
        @DisplayName("Should not break or add to blacklist when token is null or invalid")
        void shouldDoNothingWhenTokenIsNullOrInvalid() {
            String token = null;

            authService.logout(token);

            assertThat(blacklistService.isRevoked(token)).isFalse();
        }
    }
}