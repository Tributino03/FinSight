package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.Transaction;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.transaction.TransactionRequest;
import dev.Tributino.FinSight.enums.*;
import dev.Tributino.FinSight.repository.AccountRepository;
import dev.Tributino.FinSight.repository.CategoryRepository;
import dev.Tributino.FinSight.repository.TransactionRepository;
import dev.Tributino.FinSight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceIT {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoSpyBean
    private TransactionRepository transactionRepository;

    private User user;
    private Account account;
    private Category category;
    private Transaction transaction;
    private Category categoryIncome;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User(
                "Gustavo",
                "gustavo@email.com",
                "password123"
        ));

        account = accountRepository.save(new Account(
                "account",
                AccountType.CHECKING,
                new BigDecimal("900.00"),
                user
        ));

        category = categoryRepository.save(new Category(
                "food",
                CategoryType.EXPENSE,
                user
        ));

        categoryIncome = categoryRepository.save(new Category(
                "salary",
                CategoryType.INCOME,
                user
        ));

        transaction = transactionRepository.save(new Transaction(
                new BigDecimal("100.00"),
                "food",
                TransactionType.DEBIT,
                PaymentMethod.DEBIT_CARD,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                account,
                category
        ));
    }

    @Test
    @DisplayName("should rollback balance and not persist transaction in PostgreSQL when repository fails during debit")
    void shouldRollbackWhenDatabaseFailsOnCreateDebit() {
        Long accountId = account.getId();
        Long categoryId = category.getId();
        long totalTransactionsBefore = transactionRepository.count();

        doThrow(new RuntimeException("Simulated PostgreSQL connection failure"))
                .when(transactionRepository).save(any(Transaction.class));

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"),
                "food",
                LocalDateTime.now(),
                PaymentMethod.PIX
        );

        assertThatThrownBy(() -> transactionService.createDebit(
                request,
                accountId,
                categoryId,
                user
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated PostgreSQL connection failure");

        Account reloadedAccount = accountRepository.findById(accountId).orElseThrow();

        assertThat(reloadedAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("900.00"));

        assertThat(transactionRepository.count())
                .isEqualTo(totalTransactionsBefore);
    }

    @Test
    @DisplayName("should rollback balance and not persist transaction in PostgreSQL when repository fails during credit")
    void shouldRollbackWhenDatabaseFailsOnCreateCredit() {
        Long accountId = account.getId();
        Long categoryId = categoryIncome.getId();
        long totalTransactionsBefore = transactionRepository.count();

        doThrow(new RuntimeException("Simulated PostgreSQL connection failure"))
                .when(transactionRepository).save(any(Transaction.class));

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"),
                "salary",
                LocalDateTime.now(),
                PaymentMethod.PIX
        );

        assertThatThrownBy(() -> transactionService.createCredit(
                request,
                accountId,
                categoryId,
                user
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated PostgreSQL connection failure");

        Account reloadedAccount = accountRepository.findById(accountId).orElseThrow();

        assertThat(reloadedAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("900.00"));

        assertThat(transactionRepository.count())
                .isEqualTo(totalTransactionsBefore);
    }

    @Test
    @DisplayName("should rollback balance and transaction status in PostgreSQL when repository fails during cancellation")
    void shouldRollbackWhenDatabaseFailsOnCancelTransaction() {
        Long txId = transaction.getId();
        Long accId = account.getId();

        doThrow(new RuntimeException("Simulated PostgreSQL connection failure"))
                .when(transactionRepository).save(any(Transaction.class));

        assertThatThrownBy(() -> transactionService.cancelTransaction(txId, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated PostgreSQL connection failure");

        Account reloadedAccount = accountRepository.findById(accId).orElseThrow();
        Transaction reloadedTransaction = transactionRepository.findById(txId).orElseThrow();

        assertThat(reloadedAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("900.00"));
        
        assertThat(reloadedTransaction.getTransactionStatus())
                .isEqualTo(TransactionStatus.COMPLETED);
    }
}