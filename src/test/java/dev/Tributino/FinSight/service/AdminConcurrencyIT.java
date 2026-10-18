
package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.config.TestcontainersConfig;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.UserRoleUpdateRequest;
import dev.Tributino.FinSight.enums.Role;
import dev.Tributino.FinSight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class AdminConcurrencyIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminService adminService;

    private User testAdmin;
    private User testAdmin1;
    private User testAdmin2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testAdmin = new User(
                "testAdmin", "email0@gmail.com", "password"
        );
        testAdmin.changeRole(Role.ADMIN);
        testAdmin.enable();
        testAdmin = userRepository.save(testAdmin);

        testAdmin1 = new User(
                "testAdmin1", "email1@gmail.com", "password"
        );
        testAdmin1.changeRole(Role.ADMIN);
        testAdmin1.enable();
        testAdmin1 = userRepository.save(testAdmin1);

        testAdmin2 = new User(
                "testAdmin2", "email2@gmail.com", "password"
        );
        testAdmin2.changeRole(Role.ADMIN);
        testAdmin2.enable();
        testAdmin2 = userRepository.save(testAdmin2);
    }

    @Test
    void shouldThrowException_whenConcurrentDemotionsLeaveLessThanTwoAdmins()
            throws InterruptedException {

        int numberOfThreads = 2;

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch readyLatch =
                new CountDownLatch(numberOfThreads);

        CountDownLatch startLatch = new CountDownLatch(1);

        CountDownLatch doneLatch =
                new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);

        ConcurrentLinkedQueue<Exception> exceptions =
                new ConcurrentLinkedQueue<>();

        UserRoleUpdateRequest request =
                new UserRoleUpdateRequest(Role.USER);

        try {
            executorService.submit(() -> {
                readyLatch.countDown();

                try {
                    startLatch.await();

                    adminService.updateRole(
                            testAdmin,
                            testAdmin1.getId(),
                            request
                    );

                    successCount.incrementAndGet();

                } catch (Exception exception) {
                    exceptions.add(exception);

                } finally {
                    doneLatch.countDown();
                }
            });

            executorService.submit(() -> {
                readyLatch.countDown();

                try {
                    startLatch.await();

                    adminService.updateRole(
                            testAdmin,
                            testAdmin2.getId(),
                            request
                    );

                    successCount.incrementAndGet();

                } catch (Exception exception) {
                    exceptions.add(exception);

                } finally {
                    doneLatch.countDown();
                }
            });

            assertTrue(
                    readyLatch.await(10, TimeUnit.SECONDS),
                    "Threads did not become ready in time."
            );

            startLatch.countDown();

            assertTrue(
                    doneLatch.await(30, TimeUnit.SECONDS),
                    "Concurrent operations did not finish in time."
            );

        } finally {
            executorService.shutdownNow();
        }

        long activeAdmins = userRepository.findAll()
                .stream()
                .filter(user ->
                        user.getRole() == Role.ADMIN
                                && user.isEnabled()
                )
                .count();

        assertEquals(
                1,
                successCount.get(),
                "Exactly one demotion should succeed."
        );

        assertEquals(
                1,
                exceptions.size(),
                "Exactly one demotion should fail."
        );

        assertTrue(
                exceptions.stream().anyMatch(exception ->
                        exception instanceof IllegalArgumentException
                                && exception.getMessage() != null
                                && exception.getMessage().contains(
                                "At least two active administrators"
                        )
                ),
                "The rejected operation should fail because of the admin rule."
        );

        assertEquals(
                2,
                activeAdmins,
                "There should be exactly two active administrators left."
        );
    }
}
