package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.domain.PasswordResetToken;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.repository.PasswordResetTokenRepository;
import dev.Tributino.FinSight.repository.UserRepository;
import dev.Tributino.FinSight.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(PasswordResetControllerIT.ControllableClockConfig.class)
class PasswordResetControllerIT {

    private static final String FORGOT_ENDPOINT = "/api/password/forgot";
    private static final String RESET_ENDPOINT = "/api/password/reset";
    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String GENERIC_FORGOT_MESSAGE =
            "If the email is registered, you will receive password reset instructions shortly.";

    private static final String USER_EMAIL = "gustavo@finsight.dev";
    private static final String INITIAL_PASSWORD = "OldPassword@2026";
    private static final String NEW_PASSWORD = "NewPassword@2026";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @TestConfiguration
    static class ControllableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.now());
        }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> currentInstant;
        private final ZoneId zone;

        MutableClock(Instant initialInstant) {
            this(initialInstant, ZoneOffset.UTC);
        }

        private MutableClock(Instant initialInstant, ZoneId zone) {
            this.currentInstant = new AtomicReference<>(initialInstant);
            this.zone = zone;
        }

        void resetTo(Instant instant) {
            currentInstant.set(instant);
        }

        void advance(Duration duration) {
            currentInstant.updateAndGet(now -> now.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant.get(), zone);
        }

        @Override
        public Instant instant() {
            return currentInstant.get();
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MutableClock mutableClock;

    @MockitoBean
    private EmailService emailService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        reset(emailService, passwordResetTokenRepository);
        mutableClock.resetTo(Instant.now());

        passwordResetTokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private User persistEnabledUser(String email, String rawPassword) {
        User user = new User("Gustavo", email, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    private String captureSentRawToken(String expectedEmail) {
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000)).sendPasswordResetEmail(
                eq(expectedEmail),
                tokenCaptor.capture(),
                eq(Duration.ofMinutes(15))
        );
        return tokenCaptor.getValue();
    }

    private static String sha256Hex(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }

    private static String normalizeTimestamp(String json) {
        return json.replaceFirst("\"timestamp\"\\s*:\\s*\"[^\"]+\"", "\"timestamp\":\"<ignored>\"");
    }

    private static String forgotJson(String email) {
        return """
                {"email": "%s"}
                """.formatted(email);
    }

    private static String resetJson(String token, String newPassword) {
        return """
                {"token": "%s", "newPassword": "%s"}
                """.formatted(token, newPassword);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    private static String extractJwtFromLoginResponse(String jsonResponse) {
        Matcher matcher = Pattern.compile("\"[^\"]+\"\\s*:\\s*\"([^\"]+\\.[^\"]+\\.[^\"]+)\"")
                .matcher(jsonResponse);
        assertThat(matcher.find())
                .as("Expected a JWT in login response: %s", jsonResponse)
                .isTrue();
        return matcher.group(1);
    }

    @Nested
    @DisplayName("POST /api/password/forgot")
    class ForgotPasswordEndpoint {

        @Test
        @DisplayName("Should return 200 with generic message, dispatch email once and store only SHA-256 hash")
        void sendsEmailAndPersistsOnlyHashedTokenForExistingUser() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE));

            ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService, timeout(2000).times(1)).sendPasswordResetEmail(
                    eq(USER_EMAIL),
                    tokenCaptor.capture(),
                    eq(Duration.ofMinutes(15))
            );

            String rawToken = tokenCaptor.getValue();
            assertThat(rawToken).isNotBlank();

            assertThat(passwordResetTokenRepository.findByTokenHash(rawToken)).isEmpty();
            assertThat(passwordResetTokenRepository.findByTokenHash(sha256Hex(rawToken))).isPresent();
        }

        @Test
        @DisplayName("Should return identical 200 status and JSON body for existing and nonexistent emails")
        void returnsIdenticalResponseForExistingAndNonExistentEmails() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            MvcResult existingResult = mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk())
                    .andReturn();

            captureSentRawToken(USER_EMAIL);
            reset(emailService);

            MvcResult nonExistentResult = mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson("naoexiste@finsight.dev")))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(nonExistentResult.getResponse().getStatus())
                    .isEqualTo(existingResult.getResponse().getStatus());
            assertThat(nonExistentResult.getResponse().getContentAsString())
                    .isEqualTo(existingResult.getResponse().getContentAsString());

            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Should return 200 with same generic body and send no email when user is disabled")
        void returnsGeneric200AndSendsNoEmailForDisabledUser() throws Exception {
            User disabledUser = persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);
            disabledUser.disable();
            userRepository.save(disabledUser);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE));

            verify(emailService, after(300).never()).sendPasswordResetEmail(any(), any(), any());
        }

        @Test
        @DisplayName("Should normalize uppercase and padded email to locate user")
        void normalizesEmailWithSpacesAndUppercase() throws Exception {
            persistEnabledUser("joao@x.com", INITIAL_PASSWORD);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson("  JoAo@X.CoM  ")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE));

            captureSentRawToken("joao@x.com");
        }

        @Test
        @DisplayName("Should return 400 for empty body, blank email or invalid email format")
        void returns400ForInvalidPayloads() throws Exception {
            // Body vazio
            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson("   ")))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson("email-invalido")))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Should invalidate previous token when a second reset request is made")
        void invalidatesPreviousTokenWhenNewRequestIsSubmitted() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());
            String firstToken = captureSentRawToken(USER_EMAIL);
            reset(emailService);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());
            String secondToken = captureSentRawToken(USER_EMAIL);

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(firstToken, NEW_PASSWORD)))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(secondToken, NEW_PASSWORD)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should still return 200 when SMTP fails asynchronously after commit")
        void returns200EvenWhenEmailServiceThrowsException() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            doThrow(new IllegalStateException("SMTP server down"))
                    .when(emailService)
                    .sendPasswordResetEmail(any(), any(), any());

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE));

            verify(emailService, timeout(2000)).sendPasswordResetEmail(
                    eq(USER_EMAIL),
                    any(),
                    eq(Duration.ofMinutes(15))
            );
        }

        @Test
        @DisplayName("Should not send email when transaction rolls back before commit")
        void doesNotDispatchEmailWhenTransactionRollsBack() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            doThrow(new RuntimeException("Simulated database write failure"))
                    .when(passwordResetTokenRepository)
                    .save(any(PasswordResetToken.class));

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().is5xxServerError());

            verify(emailService, after(300).never()).sendPasswordResetEmail(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/password/reset")
    class ResetPasswordEndpoint {

        @Test
        @DisplayName("Full flow: forgot -> capture token -> reset (204) -> login new password (200) & old password (401)")
        void completesFullPasswordResetAndVerifiesLoginWithNewAndOldPasswords() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());

            String rawToken = captureSentRawToken(USER_EMAIL);

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(rawToken, NEW_PASSWORD)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(USER_EMAIL, NEW_PASSWORD)))
                    .andExpect(status().isOk());

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(USER_EMAIL, INITIAL_PASSWORD)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should mark all user tokens as used (used_at populated) after successful reset")
        void marksAllUserTokensAsUsedAfterSuccessfulReset() throws Exception {
            User user = persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            PasswordResetToken extraToken = new PasswordResetToken(
                    user,
                    sha256Hex("extra-residual-token"),
                    mutableClock.instant().plus(Duration.ofMinutes(15)),
                    mutableClock.instant()
            );
            passwordResetTokenRepository.save(extraToken);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());
            String rawToken = captureSentRawToken(USER_EMAIL);

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(rawToken, NEW_PASSWORD)))
                    .andExpect(status().isNoContent());

            List<PasswordResetToken> userTokens = passwordResetTokenRepository.findAll();
            assertThat(userTokens).isNotEmpty();
            assertThat(userTokens).allSatisfy(token -> assertThat(token.getUsedAt()).isNotNull());
        }

        @Test
        @DisplayName("Should reject reusing an already consumed token with 400")
        void rejectsReusingConsumedToken() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());
            String rawToken = captureSentRawToken(USER_EMAIL);

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(rawToken, NEW_PASSWORD)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(rawToken, "AnotherPass@2026")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject tampered token with 400")
        void rejectsTamperedToken() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());
            String rawToken = captureSentRawToken(USER_EMAIL);

            char lastChar = rawToken.charAt(rawToken.length() - 1);
            char alteredChar = (lastChar == 'A') ? 'B' : 'A';
            String tamperedToken = rawToken.substring(0, rawToken.length() - 1) + alteredChar;

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(tamperedToken, NEW_PASSWORD)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject expired token with 400 when clock advances past TTL")
        void rejectsExpiredTokenWhenClockAdvances() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());
            String rawToken = captureSentRawToken(USER_EMAIL);

            // Avança o relógio além dos 15 minutos de TTL
            mutableClock.advance(Duration.ofMinutes(16));

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(rawToken, NEW_PASSWORD)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return identical 400 error body for nonexistent, used and expired tokens")
        void returnsIdenticalErrorMessageForNonExistentUsedAndExpiredTokens() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            MvcResult nonExistentResult = mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson("token-inexistente-qualquer", NEW_PASSWORD)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());
            String usedToken = captureSentRawToken(USER_EMAIL);
            reset(emailService);

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(usedToken, NEW_PASSWORD)))
                    .andExpect(status().isNoContent());

            MvcResult usedResult = mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(usedToken, "AnotherPass@2026")))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());
            String expiredToken = captureSentRawToken(USER_EMAIL);

            mutableClock.advance(Duration.ofMinutes(16));

            MvcResult expiredResult = mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(expiredToken, "AnotherPass@2026")))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String nonExistentBody = normalizeTimestamp(nonExistentResult.getResponse().getContentAsString());
            String usedBody = normalizeTimestamp(usedResult.getResponse().getContentAsString());
            String expiredBody = normalizeTimestamp(expiredResult.getResponse().getContentAsString());

            assertThat(usedBody).isEqualTo(nonExistentBody);
            assertThat(expiredBody).isEqualTo(nonExistentBody);
        }

        @Test
        @DisplayName("Should return 400 for blank or missing token and invalid password lengths/byte sizes")
        void returns400ForInvalidTokenOrPasswordConstraints() throws Exception {
            // Token em branco
            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson("   ", NEW_PASSWORD)))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword": "%s"}
                                    """.formatted(NEW_PASSWORD)))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson("some-valid-looking-token", "Abc@123")))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson("some-valid-looking-token", "a".repeat(73))))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson("some-valid-looking-token", "é".repeat(40))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should not leak internal stack traces or class names in error responses")
        void doesNotLeakInternalDetailsInErrorResponses() throws Exception {
            MvcResult result = mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson("invalid-token", NEW_PASSWORD)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                    .doesNotContain("Exception")
                    .doesNotContain("dev.Tributino")
                    .doesNotContain("stackTrace")
                    .doesNotContain("java.lang");
        }

        @Test
        @DisplayName("Previous JWT remains valid after password reset until tokensValidAfter is implemented")
        void oldJwtRemainsValidAfterPasswordResetUntilTokensValidAfterIsImplemented() throws Exception {
            persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

            MvcResult loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(USER_EMAIL, INITIAL_PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn();

            String oldJwt = extractJwtFromLoginResponse(loginResult.getResponse().getContentAsString());

            mockMvc.perform(post(FORGOT_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forgotJson(USER_EMAIL)))
                    .andExpect(status().isOk());
            String rawToken = captureSentRawToken(USER_EMAIL);

            mockMvc.perform(post(RESET_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(resetJson(rawToken, NEW_PASSWORD)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + oldJwt))
                    .andExpect(status().isOk());
        }
    }
}