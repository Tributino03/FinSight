package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.transaction.TransactionRequest;
import dev.Tributino.FinSight.enums.AccountType;
import dev.Tributino.FinSight.enums.CategoryType;
import dev.Tributino.FinSight.enums.PaymentMethod;
import dev.Tributino.FinSight.repository.AccountRepository;
import dev.Tributino.FinSight.repository.CategoryRepository;
import dev.Tributino.FinSight.repository.TransactionRepository;
import dev.Tributino.FinSight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TransactionConcurrencyIT {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private Account testAccount;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(
                new User(
                        "Test User",
                        "test@finsight.dev",
                        "password123"
                )
        );

        testAccount = accountRepository.save(
                new Account(
                        "Checking Account",
                        AccountType.CHECKING,
                        new BigDecimal("100.00"),
                        testUser
                )
        );

        expenseCategory = categoryRepository.save(
                new Category(
                        "Market",
                        CategoryType.EXPENSE,
                        testUser
                )
        );
    }

    @Test
    @DisplayName("Should prevent double spending under concurrent debit requests")
    void shouldPreventDoubleSpendingUnderConcurrentDebitRequests()
            throws InterruptedException {

        int numberOfThreads = 2;

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch readyLatch =
                new CountDownLatch(numberOfThreads);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch doneLatch =
                new CountDownLatch(numberOfThreads);

        AtomicInteger successCount =
                new AtomicInteger(0);

        AtomicInteger failureCount =
                new AtomicInteger(0);

        List<Throwable> failures =
                Collections.synchronizedList(new ArrayList<>());

        TransactionRequest debitRequest =
                new TransactionRequest(
                        new BigDecimal("80.00"),
                        "Concurrent Debit Test",
                        LocalDateTime.now(),
                        PaymentMethod.DEBIT_CARD
                );

        for (int i = 0; i < numberOfThreads; i++) {

            executorService.submit(() -> {

                readyLatch.countDown();

                try {
                    startLatch.await();

                    transactionService.createDebit(
                            debitRequest,
                            testAccount.getId(),
                            expenseCategory.getId(),
                            testUser
                    );

                    successCount.incrementAndGet();

                } catch (Exception exception) {

                    failureCount.incrementAndGet();
                    failures.add(exception);

                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean allThreadsReady =
                readyLatch.await(5, TimeUnit.SECONDS);

        assertThat(allThreadsReady)
                .as("Todas as threads deveriam estar prontas")
                .isTrue();

        startLatch.countDown();

        boolean completedInTime =
                doneLatch.await(10, TimeUnit.SECONDS);

        executorService.shutdown();

        assertThat(completedInTime)
                .as("As operações concorrentes deveriam terminar dentro do limite")
                .isTrue();

        assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS))
                .as("Executor deveria ser encerrado corretamente")
                .isTrue();

        Account updatedAccount =
                accountRepository.findById(testAccount.getId())
                        .orElseThrow();

        long totalTransactionsSaved =
                transactionRepository.count();

        assertThat(successCount.get())
                .as("Exatamente uma transação deve ter sucesso")
                .isEqualTo(1);

        assertThat(failureCount.get())
                .as("Exatamente uma transação deve ser rejeitada")
                .isEqualTo(1);

        assertThat(failures)
                .hasSize(1);

        assertThat(failures.get(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance.");

        assertThat(totalTransactionsSaved)
                .as("Apenas uma transação deve ser persistida")
                .isEqualTo(1L);

        assertThat(updatedAccount.getBalance())
                .as("O saldo final da conta deve ser de R$ 20.00")
                .isEqualByComparingTo(new BigDecimal("20.00"));
    }
}