package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.PasswordResetToken;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.exception.InvalidPasswordResetTokenException;
import dev.Tributino.FinSight.repository.PasswordResetTokenRepository;
import dev.Tributino.FinSight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PasswordResetConcurrencyIT {

    private static final String USER_EMAIL = "gustavo@finsight.dev";
    private static final String INITIAL_PASSWORD = "OldPassword@2026";
    private static final String NEW_PASSWORD = "NewPassword@2026";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    private record Result<T>(T value, Throwable error) {
        boolean isSuccess() {
            return error == null;
        }
    }

    private <T> List<Result<T>> runConcurrently(int threads, Callable<T> task) throws Exception {
        return runConcurrently(Collections.nCopies(threads, task));
    }

    private <T> List<Result<T>> runConcurrently(List<Callable<T>> tasks) throws Exception {
        int threads = tasks.size();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Result<T>>> futures = new ArrayList<>();

        try {
            for (Callable<T> task : tasks) {
                futures.add(executor.submit((Callable<Result<T>>) () -> {
                    readyLatch.countDown();
                    startLatch.await();
                    try {
                        return new Result<>(task.call(), null);
                    } catch (Throwable error) {
                        return new Result<>(null, error);
                    }
                }));
            }

            assertThat(readyLatch.await(10, TimeUnit.SECONDS))
                    .as("All threads should be ready")
                    .isTrue();

            startLatch.countDown();

            List<Result<T>> results = new ArrayList<>();
            for (Future<Result<T>> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;

        } finally {
            executor.shutdownNow();
        }
    }

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        reset(emailService);
    }

    private User persistEnabledUser(String email, String rawPassword) {
        User user = new User("Gustavo", email, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    private String captureSentRawToken(String expectedEmail) {
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(3000)).sendPasswordResetEmail(
                eq(expectedEmail),
                tokenCaptor.capture(),
                eq(TOKEN_TTL)
        );
        return tokenCaptor.getValue();
    }

    private static String sha256Hex(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }

    @Test
    @DisplayName("Scenarios 1, 2 & 3: 20 concurrent resets with the same token guarantee 1 winner, atomic password update, and single-use token consumption")
    void concurrentResetWithSameTokenAllowsOnlyOneWinnerAndPreservesAtomicState() throws Exception {
        User user = persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

        passwordResetService.requestPasswordReset(USER_EMAIL);
        String rawToken = captureSentRawToken(USER_EMAIL);

        int threads = 20;
        List<String> candidatePasswords = IntStream.range(0, threads)
                .mapToObj(i -> "NewPassword@" + i + "_2026")
                .toList();

        List<Callable<String>> tasks = candidatePasswords.stream()
                .<Callable<String>>map(candidate -> () -> {
                    passwordResetService.resetPassword(rawToken, candidate);
                    return candidate;
                })
                .toList();

        List<Result<String>> results = runConcurrently(tasks);

        List<Result<String>> successes = results.stream().filter(Result::isSuccess).toList();
        List<Result<String>> failures = results.stream().filter(r -> !r.isSuccess()).toList();

        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(threads - 1);
        assertThat(failures)
                .allSatisfy(failure -> assertThat(failure.error())
                        .isInstanceOf(InvalidPasswordResetTokenException.class));

        String winningPassword = successes.getFirst().value();
        Set<String> losingPasswords = candidatePasswords.stream()
                .filter(p -> !p.equals(winningPassword))
                .collect(Collectors.toSet());

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        String persistedPasswordHash = updatedUser.getPassword();

        assertThat(passwordEncoder.matches(winningPassword, persistedPasswordHash)).isTrue();
        assertThat(passwordEncoder.matches(INITIAL_PASSWORD, persistedPasswordHash)).isFalse();
        assertThat(losingPasswords)
                .hasSize(threads - 1)
                .allSatisfy(loser -> assertThat(passwordEncoder.matches(loser, persistedPasswordHash)).isFalse());

        List<PasswordResetToken> allTokens = passwordResetTokenRepository.findAll();
        assertThat(allTokens).hasSize(1);

        PasswordResetToken consumedToken = passwordResetTokenRepository
                .findByTokenHash(sha256Hex(rawToken))
                .orElseThrow();
        assertThat(consumedToken.getUsedAt()).isNotNull();
        assertThat(consumedToken.isUsed()).isTrue();
    }

    @Test
    @DisplayName("Scenario 4: N concurrent requestPasswordReset calls for the same user throw no exceptions and document active tokens")
    void concurrentForgotRequestsForSameUserCompleteWithoutExceptions() throws Exception {
        persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

        int threads = 15;
        List<Result<Void>> results = runConcurrently(threads, () -> {
            passwordResetService.requestPasswordReset(USER_EMAIL);
            return null;
        });

        assertThat(results).allMatch(Result::isSuccess);

        verify(emailService, timeout(5000).times(threads))
                .sendPasswordResetEmail(eq(USER_EMAIL), org.mockito.ArgumentMatchers.anyString(), eq(TOKEN_TTL));

        List<PasswordResetToken> allTokens = passwordResetTokenRepository.findAll();
        long activeTokensCount = allTokens.stream()
                .filter(token -> token.getUsedAt() == null)
                .count();

        assertThat(allTokens).hasSize(threads);
        assertThat(activeTokensCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Scenario 5: Parallel forgot and reset never allow two successes with the old token and maintain consistent state")
    void parallelForgotAndResetMaintainConsistentState() throws Exception {
        User user = persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);

        passwordResetService.requestPasswordReset(USER_EMAIL);
        String oldRawToken = captureSentRawToken(USER_EMAIL);
        reset(emailService);

        List<Callable<String>> tasks = new ArrayList<>();
        tasks.add(() -> {
            passwordResetService.requestPasswordReset(USER_EMAIL);
            return "FORGOT";
        });
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> {
                passwordResetService.resetPassword(oldRawToken, NEW_PASSWORD);
                return "RESET";
            });
        }

        List<Result<String>> results = runConcurrently(tasks);

        Result<String> forgotResult = results.getFirst();
        assertThat(forgotResult.isSuccess()).isTrue();

        List<Result<String>> resetResults = results.subList(1, results.size());
        long oldTokenSuccessCount = resetResults.stream().filter(Result::isSuccess).count();
        List<Result<String>> resetFailures = resetResults.stream().filter(r -> !r.isSuccess()).toList();

        assertThat(oldTokenSuccessCount).isLessThanOrEqualTo(1);
        assertThat(resetFailures)
                .allSatisfy(failure -> assertThat(failure.error())
                        .isInstanceOf(InvalidPasswordResetTokenException.class));

        String newRawToken = captureSentRawToken(USER_EMAIL);
        assertThat(newRawToken).isNotEqualTo(oldRawToken);

        PasswordResetToken oldTokenEntity = passwordResetTokenRepository
                .findByTokenHash(sha256Hex(oldRawToken))
                .orElseThrow();
        assertThat(oldTokenEntity.getUsedAt()).isNotNull();

        User reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        if (oldTokenSuccessCount == 1) {
            assertThat(passwordEncoder.matches(NEW_PASSWORD, reloadedUser.getPassword())).isTrue();
        } else {
            assertThat(passwordEncoder.matches(INITIAL_PASSWORD, reloadedUser.getPassword())).isTrue();
            PasswordResetToken newTokenEntity = passwordResetTokenRepository
                    .findByTokenHash(sha256Hex(newRawToken))
                    .orElseThrow();
            assertThat(newTokenEntity.getUsedAt()).isNull();
        }
    }

    @Test
    @DisplayName("Scenario 6: Two concurrent resets with different active tokens for the same user mark all user tokens as used")
    void concurrentResetsWithDifferentActiveTokensForSameUserMarkAllUserTokensAsUsed() throws Exception {
        User user = persistEnabledUser(USER_EMAIL, INITIAL_PASSWORD);
        Instant now = Instant.now();

        String rawToken1 = "raw-active-token-1";
        String rawToken2 = "raw-active-token-2";
        String rawToken3Residual = "raw-active-token-3-residual";

        passwordResetTokenRepository.saveAll(List.of(
                new PasswordResetToken(user, sha256Hex(rawToken1), now.plus(TOKEN_TTL), now),
                new PasswordResetToken(user, sha256Hex(rawToken2), now.plus(TOKEN_TTL), now),
                new PasswordResetToken(user, sha256Hex(rawToken3Residual), now.plus(TOKEN_TTL), now)
        ));

        String passwordOne = "PasswordOne@2026";
        String passwordTwo = "PasswordTwo@2026";

        List<Callable<String>> tasks = List.of(
                () -> {
                    passwordResetService.resetPassword(rawToken1, passwordOne);
                    return passwordOne;
                },
                () -> {
                    passwordResetService.resetPassword(rawToken2, passwordTwo);
                    return passwordTwo;
                }
        );

        List<Result<String>> results = runConcurrently(tasks);

        long successCount = results.stream().filter(Result::isSuccess).count();
        assertThat(successCount).isGreaterThanOrEqualTo(1);

        List<PasswordResetToken> userTokens = passwordResetTokenRepository.findAll();
        assertThat(userTokens).hasSize(3);
        assertThat(userTokens).allSatisfy(token -> {
            assertThat(token.getUsedAt()).isNotNull();
            assertThat(token.isUsed()).isTrue();
        });

        User reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        String finalHash = reloadedUser.getPassword();
        assertThat(passwordEncoder.matches(INITIAL_PASSWORD, finalHash)).isFalse();
        assertThat(
                passwordEncoder.matches(passwordOne, finalHash)
                        || passwordEncoder.matches(passwordTwo, finalHash)
        ).isTrue();
    }

    @Test
    @DisplayName("Scenario 7: 20 concurrent forgot requests for 20 distinct users dispatch 20 emails with no cross-thread token leakage")
    void concurrentForgotRequestsForTwentyDistinctUsersDispatchIsolatedTokens() throws Exception {
        int userCount = 20;
        List<String> emails = IntStream.range(0, userCount)
                .mapToObj(i -> "user-" + i + "@finsight.dev")
                .toList();

        for (String email : emails) {
            persistEnabledUser(email, INITIAL_PASSWORD);
        }

        List<Callable<Void>> tasks = emails.stream()
                .<Callable<Void>>map(email -> () -> {
                    passwordResetService.requestPasswordReset(email);
                    return null;
                })
                .toList();

        List<Result<Void>> results = runConcurrently(tasks);
        assertThat(results).allMatch(Result::isSuccess);

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService, timeout(5000).times(userCount)).sendPasswordResetEmail(
                emailCaptor.capture(),
                tokenCaptor.capture(),
                eq(TOKEN_TTL)
        );

        List<String> capturedEmails = emailCaptor.getAllValues();
        List<String> capturedRawTokens = tokenCaptor.getAllValues();

        assertThat(capturedEmails).containsExactlyInAnyOrderElementsOf(emails);
        assertThat(new HashSet<>(capturedRawTokens)).hasSize(userCount);

        Map<String, String> emailToRawToken = new HashMap<>();
        for (int i = 0; i < userCount; i++) {
            emailToRawToken.put(capturedEmails.get(i), capturedRawTokens.get(i));
        }

        for (Map.Entry<String, String> entry : emailToRawToken.entrySet()) {
            String expectedEmail = entry.getKey();
            String rawToken = entry.getValue();

            PasswordResetToken persistedToken = passwordResetTokenRepository
                    .findByTokenHash(sha256Hex(rawToken))
                    .orElseThrow();

            assertThat(persistedToken.getUsedAt()).isNull();

            User tokenOwner = userRepository
                    .findById(persistedToken.getUser().getId())
                    .orElseThrow();

            assertThat(tokenOwner.getEmail()).isEqualTo(expectedEmail);
        }
    }
}