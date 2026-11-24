package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.PasswordResetToken;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.event.PasswordResetRequestedEvent;
import dev.Tributino.FinSight.exception.InternalProcessingException;
import dev.Tributino.FinSight.exception.InvalidPasswordResetTokenException;
import dev.Tributino.FinSight.repository.PasswordResetTokenRepository;
import dev.Tributino.FinSight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T22:00:00Z");
    private static final Duration EXPECTED_TTL = Duration.ofMinutes(15);
    private static final String USER_EMAIL = "gustavo@finsight.dev";
    private static final String OLD_ENCODED_PASSWORD = "$2a$10$oldEncodedPasswordHash";
    private static final String SHA256_OF_ABC =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Clock fixedClock;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new PasswordResetService(
                passwordResetTokenRepository,
                userRepository,
                passwordEncoder,
                fixedClock,
                eventPublisher
        );
    }

    private User createUser(Long id, String email) {
        User user = new User("Gustavo", email, OLD_ENCODED_PASSWORD);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static String sha256Hex(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }

    @Nested
    @DisplayName("requestPasswordReset")
    class RequestPasswordReset {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("Should invalidate previous tokens, save hashed token and publish event for existing user")
            void invalidatesSavesAndPublishesForExistingUser() throws Exception {
                User user = createUser(1L, USER_EMAIL);
                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));

                service.requestPasswordReset(USER_EMAIL);

                ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
                ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor =
                        ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);

                InOrder order = inOrder(passwordResetTokenRepository, eventPublisher);
                order.verify(passwordResetTokenRepository).invalidateActiveByUser(1L, NOW);
                order.verify(passwordResetTokenRepository).save(tokenCaptor.capture());
                order.verify(eventPublisher).publishEvent(eventCaptor.capture());

                PasswordResetToken savedToken = tokenCaptor.getValue();
                PasswordResetRequestedEvent publishedEvent = eventCaptor.getValue();

                assertThat(savedToken.getUser()).isSameAs(user);
                assertThat(savedToken.getCreatedAt()).isEqualTo(NOW);
                assertThat(savedToken.getExpiresAt()).isEqualTo(NOW.plus(EXPECTED_TTL));
                assertThat(savedToken.getUsedAt()).isNull();

                assertThat(publishedEvent.email()).isEqualTo(USER_EMAIL);
                assertThat(publishedEvent.ttl()).isEqualTo(EXPECTED_TTL);
                assertThat(publishedEvent.rawToken()).isNotBlank();

                assertThat(savedToken.getTokenHash()).isNotEqualTo(publishedEvent.rawToken());
                assertThat(savedToken.getTokenHash()).isEqualTo(sha256Hex(publishedEvent.rawToken()));
            }

            @Test
            @DisplayName("Should trim and lowercase raw email before querying repository")
            void normalizesEmailBeforeLookup() {
                User user = createUser(1L, "joao@x.com");
                when(userRepository.findByEmail("joao@x.com")).thenReturn(Optional.of(user));

                service.requestPasswordReset("  Joao@X.COM ");

                verify(userRepository).findByEmail("joao@x.com");
                verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
                verify(eventPublisher).publishEvent(any(PasswordResetRequestedEvent.class));
            }

            @Test
            @DisplayName("Should generate distinct raw tokens and hashes across consecutive requests")
            void generatesDistinctTokensOnConsecutiveCalls() {
                User user = createUser(1L, USER_EMAIL);
                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));

                service.requestPasswordReset(USER_EMAIL);
                service.requestPasswordReset(USER_EMAIL);

                ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
                ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor =
                        ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);

                verify(passwordResetTokenRepository, times(2)).save(tokenCaptor.capture());
                verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

                List<PasswordResetToken> savedTokens = tokenCaptor.getAllValues();
                List<PasswordResetRequestedEvent> events = eventCaptor.getAllValues();

                assertThat(events.get(0).rawToken()).isNotEqualTo(events.get(1).rawToken());
                assertThat(savedTokens.get(0).getTokenHash()).isNotEqualTo(savedTokens.get(1).getTokenHash());
            }
        }

        @Nested
        @DisplayName("Failure and Edge Cases")
        class FailureAndEdgeCases {

            @Test
            @DisplayName("Should silently return without side effects when email does not exist")
            void doesNothingWhenEmailNotFound() {
                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

                service.requestPasswordReset(USER_EMAIL);

                verify(passwordResetTokenRepository, never()).invalidateActiveByUser(any(), any());
                verify(passwordResetTokenRepository, never()).save(any());
                verifyNoInteractions(eventPublisher);
            }

            @Test
            @DisplayName("Should silently return without side effects when user is disabled")
            void doesNothingWhenUserIsDisabled() {
                User disabledUser = createUser(1L, USER_EMAIL);
                disabledUser.disable();
                when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(disabledUser));

                service.requestPasswordReset(USER_EMAIL);

                verify(passwordResetTokenRepository, never()).invalidateActiveByUser(any(), any());
                verify(passwordResetTokenRepository, never()).save(any());
                verifyNoInteractions(eventPublisher);
            }

            @Test
            @DisplayName("Should throw NullPointerException when rawEmail is null (guarded earlier by DTO @NotBlank)")
            void throwsNullPointerExceptionWhenEmailIsNull() {
                assertThatThrownBy(() -> service.requestPasswordReset(null))
                        .isInstanceOf(NullPointerException.class);

                verifyNoInteractions(userRepository, passwordResetTokenRepository, eventPublisher);
            }
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Nested
        @DisplayName("Success")
        class Success {

            @Test
            @DisplayName("Should consume token, invalidate residual tokens, encode password and update user in strict order")
            void resetsPasswordInStrictOrder() {
                User user = createUser(1L, USER_EMAIL);
                String rawToken = "abc";
                String newRawPassword = "NewSecurePassword@2026";
                String newEncodedPassword = "$2a$10$newEncodedPasswordHash";

                PasswordResetToken storedToken = new PasswordResetToken(
                        user,
                        SHA256_OF_ABC,
                        NOW.plus(EXPECTED_TTL),
                        NOW.minus(Duration.ofMinutes(2))
                );

                when(passwordResetTokenRepository.findByTokenHash(SHA256_OF_ABC))
                        .thenReturn(Optional.of(storedToken));
                when(passwordResetTokenRepository.consume(SHA256_OF_ABC, NOW)).thenReturn(1);
                when(userRepository.findById(1L)).thenReturn(Optional.of(user));
                when(passwordEncoder.encode(newRawPassword)).thenReturn(newEncodedPassword);

                service.resetPassword(rawToken, newRawPassword);

                InOrder order = inOrder(passwordResetTokenRepository, userRepository, passwordEncoder);
                order.verify(passwordResetTokenRepository).findByTokenHash(SHA256_OF_ABC);
                order.verify(passwordResetTokenRepository).consume(SHA256_OF_ABC, NOW);
                order.verify(passwordResetTokenRepository).invalidateActiveByUser(1L, NOW);
                order.verify(userRepository).findById(1L);
                order.verify(passwordEncoder).encode(newRawPassword);

                assertThat(user.getPassword()).isEqualTo(newEncodedPassword);
                assertThat(user.getPassword()).isNotEqualTo(newRawPassword);
            }

            @Test
            @DisplayName("Should accept a password with exactly 72 UTF-8 bytes")
            void acceptsPasswordAtExact72BytesBoundary() {
                User user = createUser(1L, USER_EMAIL);
                String rawToken = "abc";
                String password72Bytes = "a".repeat(72);

                PasswordResetToken storedToken = new PasswordResetToken(
                        user, SHA256_OF_ABC, NOW.plus(EXPECTED_TTL), NOW
                );

                when(passwordResetTokenRepository.findByTokenHash(SHA256_OF_ABC))
                        .thenReturn(Optional.of(storedToken));
                when(passwordResetTokenRepository.consume(SHA256_OF_ABC, NOW)).thenReturn(1);
                when(userRepository.findById(1L)).thenReturn(Optional.of(user));
                when(passwordEncoder.encode(password72Bytes)).thenReturn("$2a$10$encoded72");

                service.resetPassword(rawToken, password72Bytes);

                verify(passwordEncoder).encode(password72Bytes);
                assertThat(user.getPassword()).isEqualTo("$2a$10$encoded72");
            }
        }

        @Nested
        @DisplayName("Failure")
        class Failure {

            @Test
            @DisplayName("Should throw InvalidPasswordResetTokenException when token hash is not found")
            void throwsWhenTokenDoesNotExist() {
                when(passwordResetTokenRepository.findByTokenHash(SHA256_OF_ABC))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.resetPassword("abc", "NewPass@123"))
                        .isInstanceOf(InvalidPasswordResetTokenException.class);

                verify(passwordResetTokenRepository, never()).consume(any(), any());
                verifyNoInteractions(passwordEncoder, userRepository);
            }

            @Test
            @DisplayName("Should throw InvalidPasswordResetTokenException and preserve user password when consume returns 0")
            void throwsWhenTokenIsExpiredOrAlreadyUsed() {
                User user = createUser(1L, USER_EMAIL);
                PasswordResetToken expiredToken = new PasswordResetToken(
                        user,
                        SHA256_OF_ABC,
                        NOW.minusSeconds(1),
                        NOW.minus(EXPECTED_TTL)
                );

                when(passwordResetTokenRepository.findByTokenHash(SHA256_OF_ABC))
                        .thenReturn(Optional.of(expiredToken));
                when(passwordResetTokenRepository.consume(SHA256_OF_ABC, NOW)).thenReturn(0);

                assertThatThrownBy(() -> service.resetPassword("abc", "NewPass@123"))
                        .isInstanceOf(InvalidPasswordResetTokenException.class);

                verify(passwordResetTokenRepository, never()).invalidateActiveByUser(any(), any());
                verifyNoInteractions(passwordEncoder, userRepository);
                assertThat(user.getPassword()).isEqualTo(OLD_ENCODED_PASSWORD);
            }

            @Test
            @DisplayName("Should throw identical exception message for nonexistent and expired tokens")
            void throwsIdenticalErrorMessageToPreventTokenEnumeration() {
                User user = createUser(1L, USER_EMAIL);
                PasswordResetToken expiredToken = new PasswordResetToken(
                        user, SHA256_OF_ABC, NOW.minusSeconds(1), NOW.minus(EXPECTED_TTL)
                );

                when(passwordResetTokenRepository.findByTokenHash(SHA256_OF_ABC))
                        .thenReturn(Optional.empty());
                InvalidPasswordResetTokenException notFoundEx = catchThrowableOfType(
                        () -> service.resetPassword("abc", "NewPass@123"),
                        InvalidPasswordResetTokenException.class
                );

                when(passwordResetTokenRepository.findByTokenHash(SHA256_OF_ABC))
                        .thenReturn(Optional.of(expiredToken));
                when(passwordResetTokenRepository.consume(SHA256_OF_ABC, NOW)).thenReturn(0);
                InvalidPasswordResetTokenException expiredEx = catchThrowableOfType(
                        () -> service.resetPassword("abc", "NewPass@123"),
                        InvalidPasswordResetTokenException.class
                );

                assertThat(notFoundEx).isNotNull();
                assertThat(expiredEx).isNotNull();
                assertThat(notFoundEx.getMessage()).isEqualTo(expiredEx.getMessage());
            }

            @Test
            @DisplayName("Should reject 73 ASCII bytes before touching any repository")
            void rejectsPasswordWith73AsciiBytes() {
                String password73Bytes = "a".repeat(73);

                assertThatThrownBy(() -> service.resetPassword("abc", password73Bytes))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("72 bytes");

                verifyNoInteractions(passwordResetTokenRepository, userRepository, passwordEncoder);
            }

            @Test
            @DisplayName("Should reject 40 multibyte characters (80 UTF-8 bytes) before touching any repository")
            void rejectsMultibytePasswordExceeding72Utf8Bytes() {
                String password40Chars80Bytes = "é".repeat(40);
                assertThat(password40Chars80Bytes).hasSize(40);
                assertThat(password40Chars80Bytes.getBytes(StandardCharsets.UTF_8)).hasSize(80);

                assertThatThrownBy(() -> service.resetPassword("abc", password40Chars80Bytes))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("72 bytes");

                verifyNoInteractions(passwordResetTokenRepository, userRepository, passwordEncoder);
            }

            @Test
            @DisplayName("Should throw InternalProcessingException if user disappears between consume and findById")
            void throwsInternalProcessingExceptionWhenUserVanishesMidTransaction() {
                User user = createUser(1L, USER_EMAIL);
                PasswordResetToken storedToken = new PasswordResetToken(
                        user, SHA256_OF_ABC, NOW.plus(EXPECTED_TTL), NOW
                );

                when(passwordResetTokenRepository.findByTokenHash(SHA256_OF_ABC))
                        .thenReturn(Optional.of(storedToken));
                when(passwordResetTokenRepository.consume(SHA256_OF_ABC, NOW)).thenReturn(1);
                when(userRepository.findById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.resetPassword("abc", "NewPass@123"))
                        .isInstanceOf(InternalProcessingException.class);

                verifyNoInteractions(passwordEncoder);
            }
        }
    }
}