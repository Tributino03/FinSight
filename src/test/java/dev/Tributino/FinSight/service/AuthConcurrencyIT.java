package dev.Tributino.FinSight.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import dev.Tributino.FinSight.config.TestcontainersConfig;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public class AuthConcurrencyIT {

    private static final String USER_EMAIL = "auth-it@finsight.dev";
    private static final String USER_PASSWORD = "123456";
    private static final String WRONG_PASSWORD = "wrong-password";
    private static final String BLACKLIST_PATTERN = "jwt:blacklist:*";
    private static final int MAX_EXTRA_USERS = 20;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenBlacklistService blacklistService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private User user;


    @BeforeEach
    void setUp() {
        cleanUp();
        user = createUser(USER_EMAIL);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        Set<String> keys = stringRedisTemplate.keys(BLACKLIST_PATTERN);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

        userRepository.findByEmail(USER_EMAIL).ifPresent(userRepository::delete);

        for (int i = 0; i < MAX_EXTRA_USERS; i++) {
            userRepository.findByEmail(emailOf(i)).ifPresent(userRepository::delete);
        }
    }

    private String emailOf(int index) {
        return "user" + index + "@auth-it.dev";
    }

    private User createUser(String email) {
        String encodedPassword = passwordEncoder.encode(USER_PASSWORD);
        return userRepository.save(new User("user", email, encodedPassword));
    }

    private String login(String email) {
        return authService.login(email, USER_PASSWORD);
    }

    private DecodedJWT decode(String token) {
        DecodedJWT jwt = tokenService.decodeToken(token);
        assertThat(jwt).as("token should be valid and decodable").isNotNull();
        return jwt;
    }

    private int blacklistSize() {
        Set<String> keys = stringRedisTemplate.keys(BLACKLIST_PATTERN);
        return keys == null ? 0 : keys.size();
    }

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

    @Nested
    @DisplayName("Concurrent login")
    class ConcurrentLogin {

        @Test
        @DisplayName("Should generate a unique jti for 50 simultaneous logins of the same user")
        void sameUserGeneratesUniqueJtis() throws Exception {
            int logins = 50;

            List<Result<String>> results =
                    runConcurrently(logins, () -> login(USER_EMAIL));

            assertThat(results).allMatch(Result::isSuccess);

            Set<String> jtis = results.stream()
                    .map(result -> decode(result.value()).getId())
                    .collect(Collectors.toSet());

            assertThat(jtis)
                    .as("every token must have its own jti")
                    .hasSize(logins);
        }

        @Test
        @DisplayName("Should return each user's own token when different users log in simultaneously")
        void differentUsersDoNotLeakTokens() throws Exception {
            int users = 10;
            List<Callable<String>> tasks = new ArrayList<>();

            for (int i = 0; i < users; i++) {
                String email = emailOf(i);
                createUser(email);
                tasks.add(() -> login(email));
            }

            List<Result<String>> results = runConcurrently(tasks);

            for (int i = 0; i < users; i++) {
                Result<String> result = results.get(i);

                assertThat(result.error()).isNull();
                assertThat(decode(result.value()).getSubject())
                        .as("token of user %d must belong to user %d", i, i)
                        .isEqualTo(emailOf(i));
            }
        }

        @Test
        @DisplayName("Should reject all simultaneous logins with wrong password and issue no token")
        void wrongPasswordRejectedForAll() throws Exception {
            List<Result<String>> results =
                    runConcurrently(20, () -> authService.login(USER_EMAIL, WRONG_PASSWORD));

            assertThat(results).allMatch(result -> result.value() == null);
            assertThat(results).allMatch(result -> result.error() instanceof BadCredentialsException);
        }

        @Test
        @DisplayName("Should not let valid and invalid concurrent logins interfere with each other")
        void mixedValidAndInvalidLogins() throws Exception {
            List<Callable<String>> tasks = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                if (i % 2 == 0) {
                    tasks.add(() -> login(USER_EMAIL));
                } else {
                    tasks.add(() -> authService.login(USER_EMAIL, WRONG_PASSWORD));
                }
            }

            List<Result<String>> results = runConcurrently(tasks);

            for (int i = 0; i < results.size(); i++) {
                Result<String> result = results.get(i);

                if (i % 2 == 0) {
                    assertThat(result.error()).isNull();
                    assertThat(decode(result.value()).getSubject()).isEqualTo(user.getEmail());
                } else {
                    assertThat(result.value()).isNull();
                    assertThat(result.error()).isInstanceOf(BadCredentialsException.class);
                }
            }
        }
    }

    @Nested
    @DisplayName("Concurrent logout")
    class ConcurrentLogout {

        @Test
        @DisplayName("Should be idempotent when the same token is revoked by 20 simultaneous logouts")
        void sameTokenLogoutIsIdempotent() throws Exception {
            String token = login(USER_EMAIL);
            String jti = decode(token).getId();

            List<Result<Object>> results = runConcurrently(20, () -> {
                authService.logout(token);
                return null;
            });

            assertThat(results).allMatch(Result::isSuccess);
            assertThat(blacklistService.isRevoked(jti)).isTrue();
            assertThat(blacklistSize())
                    .as("only one key should exist in Redis")
                    .isEqualTo(1);

            Long ttl = stringRedisTemplate.getExpire("jwt:blacklist:" + jti, TimeUnit.SECONDS);
            assertThat(ttl)
                    .as("TTL must be positive and at most 2 hours")
                    .isPositive()
                    .isLessThanOrEqualTo(7200L);
        }

        @Test
        @DisplayName("Should revoke only each user's own token when many users log out in parallel")
        void eachUserRevokesOnlyOwnToken() throws Exception {
            int users = 10;
            List<String> jtis = new ArrayList<>();
            List<Callable<Object>> tasks = new ArrayList<>();

            for (int i = 0; i < users; i++) {
                String email = emailOf(i);
                createUser(email);

                String token = login(email);
                jtis.add(decode(token).getId());

                if (i % 2 == 0) {
                    tasks.add(() -> {
                        authService.logout(token);
                        return null;
                    });
                }
            }

            List<Result<Object>> results = runConcurrently(tasks);

            assertThat(results).allMatch(Result::isSuccess);

            for (int i = 0; i < users; i++) {
                boolean shouldBeRevoked = (i % 2 == 0);
                assertThat(blacklistService.isRevoked(jtis.get(i)))
                        .as("user %d revoked?", i)
                        .isEqualTo(shouldBeRevoked);
            }

            assertThat(blacklistSize()).isEqualTo(users / 2);
        }

        @Test
        @DisplayName("Should never accept a revoked token after logout completes")
        void revokedTokenIsNeverAcceptedAfterLogout() throws Exception {
            String token = login(USER_EMAIL);
            String jti = decode(token).getId();

            authService.logout(token);

            List<Result<Boolean>> results =
                    runConcurrently(50, () -> blacklistService.isRevoked(jti));

            assertThat(results).allMatch(Result::isSuccess);
            assertThat(results)
                    .as("no check may find the token as not revoked")
                    .allMatch(result -> Boolean.TRUE.equals(result.value()));
        }
    }

    @Nested
    @DisplayName("Concurrent login and logout")
    class ConcurrentLoginAndLogout {

        @Test
        @DisplayName("Should keep a control token valid while 20 other tokens are revoked concurrently")
        void revokingOthersDoesNotAffectControlToken() throws Exception {
            int tokensToRevoke = 20;

            String controlToken = login(USER_EMAIL);
            String controlJti = decode(controlToken).getId();

            List<String> revokedJtis = new ArrayList<>();
            List<Callable<Object>> tasks = new ArrayList<>();

            for (int i = 0; i < tokensToRevoke; i++) {
                String token = login(USER_EMAIL);
                revokedJtis.add(decode(token).getId());

                tasks.add(() -> {
                    authService.logout(token);
                    return null;
                });
            }

            int newLogins = 20;
            int firstLoginTaskIndex = tasks.size();
            for (int i = 0; i < newLogins; i++) {
                tasks.add(() -> login(USER_EMAIL));
            }

            List<Result<Object>> results = runConcurrently(tasks);

            assertThat(results).allMatch(Result::isSuccess);

            assertThat(blacklistService.isRevoked(controlJti))
                    .as("control token must remain valid")
                    .isFalse();

            assertThat(revokedJtis)
                    .allMatch(blacklistService::isRevoked);

            for (int i = firstLoginTaskIndex; i < results.size(); i++) {
                String newToken = (String) results.get(i).value();
                assertThat(blacklistService.isRevoked(decode(newToken).getId()))
                        .as("tokens issued during the logouts must not be revoked")
                        .isFalse();
            }

            assertThat(blacklistSize()).isEqualTo(tokensToRevoke);
        }
    }
}