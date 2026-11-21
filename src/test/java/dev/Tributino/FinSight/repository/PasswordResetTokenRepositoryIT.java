package dev.Tributino.FinSight.repository;

import dev.Tributino.FinSight.config.TestcontainersConfig;
import dev.Tributino.FinSight.domain.PasswordResetToken;
import dev.Tributino.FinSight.domain.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public class PasswordResetTokenRepositoryIT {

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PlatformTransactionManager platformTransactionManager;

    private TransactionTemplate transactionTemplate;
    private User user;
    private static final String USER_EMAIL = "prt-it@finsight.dev";
    private static final String TOKEN_HASH = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
    private static final String OTHER_USER_EMAIL = "other-prt-it@finsight.dev";

    @BeforeEach
    void setUp() {
        cleanUp();
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);

        user = createUser(USER_EMAIL);
    }

    @AfterEach
    public void cleanUp() {
        userRepository.findByEmail(USER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(OTHER_USER_EMAIL).ifPresent(userRepository::delete);
    }

    public User createUser(String email) {
        User user = new User("userTest", email, "1234456");
        return userRepository.save(user);
    }

    private PasswordResetToken createToken(String hash, Instant expiresAt, Instant createdAt) {
        PasswordResetToken passwordResetToken = new PasswordResetToken(this.user, hash, expiresAt, createdAt);
        return passwordResetTokenRepository.save(passwordResetToken);
    }

    private PasswordResetToken createToken(User targetUser, String hash, Instant expiresAt, Instant createdAt) {
        PasswordResetToken passwordResetToken = new PasswordResetToken(targetUser, hash, expiresAt, createdAt);
        return passwordResetTokenRepository.save(passwordResetToken);
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
    @DisplayName("consume")
    class Consume {
        @Test
        @DisplayName("Should let exactly one of 20 simultaneous consumes win")
        void onlyOneConcurrentConsumeWins() throws Exception {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            createToken(TOKEN_HASH, now.plus(15, ChronoUnit.MINUTES), now);
            int attempts = 20;

            List<Result<Integer>> results = runConcurrently(attempts, () ->
                    transactionTemplate.execute(status -> passwordResetTokenRepository.consume(TOKEN_HASH, now))
            );

            assertThat(results).allMatch(Result::isSuccess);

            assertThat(results)
                    .extracting(Result::value)
                    .filteredOn(updated -> updated == 1)
                    .hasSize(1);

            assertThat(results)
                    .extracting(Result::value)
                    .filteredOn(updated -> updated == 0)
                    .hasSize(19);

            PasswordResetToken consumed = passwordResetTokenRepository.findByTokenHash(TOKEN_HASH).orElseThrow();
            assertThat(consumed.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return 0 and preserve original usedAt when token is already used")
        void secondConsumeReturnsZero() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            createToken(TOKEN_HASH, now.plus(15, ChronoUnit.MINUTES), now);

            Integer firstResult = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.consume(TOKEN_HASH, now));
            assertThat(firstResult).isEqualTo(1);

            Instant originalUsedAt = passwordResetTokenRepository.findByTokenHash(TOKEN_HASH)
                    .orElseThrow()
                    .getUsedAt();
            assertThat(originalUsedAt).isNotNull();

            Instant later = now.plusSeconds(10);
            Integer secondResult = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.consume(TOKEN_HASH, later));
            assertThat(secondResult).isZero();

            Instant usedAtAfterSecondAttempt = passwordResetTokenRepository.findByTokenHash(TOKEN_HASH)
                    .orElseThrow()
                    .getUsedAt();
            assertThat(usedAtAfterSecondAttempt).isEqualTo(originalUsedAt);
        }

        @Test
        @DisplayName("Should return 0 when the token is expired")
        void expiredTokenReturnsZero() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            createToken(TOKEN_HASH, now.minusSeconds(1), now.minus(15, ChronoUnit.MINUTES));

            Integer result = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.consume(TOKEN_HASH, now));

            assertThat(result).isZero();

            PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TOKEN_HASH).orElseThrow();
            assertThat(token.getUsedAt()).isNull();
        }

        @Test
        @DisplayName("Should return 0 when expiresAt is exactly equal to now (strict > boundary)")
        void tokenExpiringAtExactNowReturnsZero() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            createToken(TOKEN_HASH, now, now.minus(15, ChronoUnit.MINUTES));

            Integer result = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.consume(TOKEN_HASH, now));

            assertThat(result).isZero();

            PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TOKEN_HASH).orElseThrow();
            assertThat(token.getUsedAt()).isNull();
        }

        @Test
        @DisplayName("Should return 0 when the token does not exist")
        void unknownTokenReturnsZero() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            createToken("fiu9j0qdwnhu3q8u4383249i0309num", now.plus(15, ChronoUnit.MINUTES), now);

            Integer result = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.consume(TOKEN_HASH, now));

            assertThat(result).isZero();
            assertThat(passwordResetTokenRepository.findByTokenHash(TOKEN_HASH)).isEmpty();
        }

        @Test
        @DisplayName("Should consume a valid token and return 1")
        void consumesValidToken() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            createToken(TOKEN_HASH, now.plus(15, ChronoUnit.MINUTES), now);

            Integer result = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.consume(TOKEN_HASH, now));

            assertThat(result).isEqualTo(1);

            PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TOKEN_HASH).orElseThrow();
            assertThat(token.getUsedAt()).isEqualTo(now.truncatedTo(ChronoUnit.MICROS));
        }

    }

    @Nested
    @DisplayName("invalidateActiveByUser")
    class InvalidateActiveByUser {
        @Test
        @DisplayName("Should mark all active tokens of the user as used")
        void invalidatesAllActiveTokens() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            String firstHash = "fiu90qdwnhu3q8u4383249i0309num";
            String secondHash = "33434vefsfdswfewfcecfew";

            createToken(firstHash, now.plus(15, ChronoUnit.MINUTES), now);
            createToken(secondHash, now.plus(15, ChronoUnit.MINUTES), now);

            Integer invalidated = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.invalidateActiveByUser(user.getId(), now)
            );

            assertThat(invalidated).isEqualTo(2);

            PasswordResetToken firstReloaded = passwordResetTokenRepository.findByTokenHash(firstHash).orElseThrow();
            PasswordResetToken secondReloaded = passwordResetTokenRepository.findByTokenHash(secondHash).orElseThrow();

            assertThat(firstReloaded.getUsedAt()).isNotNull();
            assertThat(secondReloaded.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not touch tokens of other users")
        void doesNotAffectOtherUsers() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            User otherUser = createUser(OTHER_USER_EMAIL);
            String otherUserHash = "b".repeat(64);

            createToken(user, TOKEN_HASH, now.plus(15, ChronoUnit.MINUTES), now);
            createToken(otherUser, otherUserHash, now.plus(15, ChronoUnit.MINUTES), now);

            Integer invalidated = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.invalidateActiveByUser(user.getId(), now)
            );

            assertThat(invalidated).isEqualTo(1);

            PasswordResetToken mainUserToken = passwordResetTokenRepository.findByTokenHash(TOKEN_HASH).orElseThrow();
            PasswordResetToken otherUserToken = passwordResetTokenRepository.findByTokenHash(otherUserHash).orElseThrow();

            assertThat(mainUserToken.getUsedAt()).isNotNull();
            assertThat(otherUserToken.getUsedAt()).isNull();
        }

        @Test
        @DisplayName("Should make consume return 0 afterwards")
        void consumedAfterInvalidationReturnsZero() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            createToken(TOKEN_HASH, now.plus(15, ChronoUnit.MINUTES), now);

            Integer invalidated = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.invalidateActiveByUser(user.getId(), now));

            assertThat(invalidated).isEqualTo(1);

            Integer result = transactionTemplate.execute(status ->
                    passwordResetTokenRepository.consume(TOKEN_HASH, now));

            assertThat(result).isZero();

        }
    }

    @Nested
    @DisplayName("concurrency between consume and invalidate")
    class ConsumeVsInvalidate {
        @Test
        @DisplayName("Should never let both consume and invalidate win the same token")
        void consumeAndInvalidateRaceHasSingleWinner() throws Exception {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            createToken(TOKEN_HASH, now.plus(15, ChronoUnit.MINUTES), now);

            Callable<Integer> consumeTask = () -> transactionTemplate.execute(status ->
                    passwordResetTokenRepository.consume(TOKEN_HASH, now)
            );

            Callable<Integer> invalidateTask = () -> transactionTemplate.execute(status ->
                    passwordResetTokenRepository.invalidateActiveByUser(user.getId(), now)
            );

            List<Result<Integer>> results = runConcurrently(List.of(consumeTask, invalidateTask));

            assertThat(results).allMatch(Result::isSuccess);

            assertThat(results)
                    .extracting(Result::value)
                    .containsExactlyInAnyOrder(1, 0);

            PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TOKEN_HASH).orElseThrow();
            assertThat(token.getUsedAt()).isNotNull();
        }
    }
}