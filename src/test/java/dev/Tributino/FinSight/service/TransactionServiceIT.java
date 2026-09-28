package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.Transaction;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.transaction.TransactionRequest;
import dev.Tributino.FinSight.dto.transaction.TransactionResponse;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

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
    private Category categoryExpense;
    private Category categoryIncome;
    private Transaction transaction;

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

        categoryExpense = categoryRepository.save(new Category(
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
                categoryExpense
        ));
    }

    @Test
    @DisplayName("Should create debit and reduce account balance")
    void shouldCreateDebitAndReduceAccountBalance() {
        TransactionRequest transactionRequest = new TransactionRequest(
                new BigDecimal("100.00"),
                "food",
                LocalDateTime.now(),
                PaymentMethod.DEBIT_CARD
        );

        TransactionResponse transactionResponse =
                transactionService.createDebit(
                        transactionRequest,
                        account.getId(),
                        categoryExpense.getId(),
                        user
        );

        Account updatedAccount =
                accountRepository.findById(account.getId())
                        .orElseThrow();

        assertThat(transactionResponse).isNotNull();

        assertThat(updatedAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("800.00"));

        assertThat(transactionRepository.count()).isEqualTo(2);

        Transaction persistedTransaction = transactionRepository.findById(transactionResponse.id())
                .orElseThrow(() -> new AssertionError("The transaction was not found in the database"));

        assertThat(persistedTransaction.getTransactionType())
                .as("The transaction type should be DEBIT")
                .isEqualTo(TransactionType.DEBIT);

        assertThat(persistedTransaction.getTransactionStatus())
                .as("The transaction status should be COMPLETED")
                .isEqualTo(TransactionStatus.COMPLETED);

        assertThat(persistedTransaction.getAmount())
                .as("The transaction amount should be exactly 100.00")
                .isEqualByComparingTo(new BigDecimal("100.00"));

        assertThat(persistedTransaction.getAccount().getId())
                .as("The transaction should be linked to the correct account")
                .isEqualTo(account.getId());

        assertThat(persistedTransaction.getCategory().getId())
                .as("The transaction should be linked to the correct categorycategory")
                .isEqualTo(categoryExpense.getId());

    }

    @Test
    @DisplayName("Should throw exception when debit amount exceeds available balance")
    void shouldThrowExceptionWhenDebitExceedsBalance() {
        TransactionRequest transactionRequest = new TransactionRequest(
                new BigDecimal("950.00"),
                "food",
                LocalDateTime.now(),
                PaymentMethod.DEBIT_CARD
        );

        long transactionsBefore = transactionRepository.count();

        assertThatThrownBy(() -> transactionService.createDebit(
                transactionRequest,
                account.getId(),
                categoryExpense.getId(),
                user
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance.");

        Account updatedAccount =
                accountRepository.findById(account.getId())
                        .orElseThrow();

        assertThat(updatedAccount.getBalance())
                .as("The account balance should remain unchanged")
                .isEqualByComparingTo(new BigDecimal("900.00"));

        assertThat(transactionRepository.count())
                .as("No new transaction should be saved to the database")
                .isEqualTo(transactionsBefore);
    }

    @Test
    @DisplayName("Should reject debit when using an INCOME category")
    void shouldRejectDebitWithIncomeCategory() {
        TransactionRequest transactionRequest = new TransactionRequest(
                new BigDecimal("100.00"),
                "food",
                LocalDateTime.now(),
                PaymentMethod.DEBIT_CARD
        );

        long transactionsBefore = transactionRepository.count();

        assertThatThrownBy(() -> transactionService.createDebit(
                transactionRequest,
                account.getId(),
                categoryIncome.getId(),
                user
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Debit transactions require an EXPENSE category.");

        Account updatedAccount =
                accountRepository.findById(account.getId())
                        .orElseThrow();

        assertThat(updatedAccount.getBalance())
                .as("The account balance should remain unchanged")
                .isEqualByComparingTo("900.00");

        assertThat(transactionRepository.count())
                .as("No new transaction should be saved to the database")
                .isEqualTo(transactionsBefore);
    }

    @Test
    @DisplayName("Should reject credit when using an EXPENSE category")
    void shouldRejectCreditWithExpenseCategory() {
        TransactionRequest transactionRequest = new TransactionRequest(
                new BigDecimal("9000.75"),
                "salary",
                LocalDateTime.now(),
                PaymentMethod.PIX
        );

        Long transactionBefore = transactionRepository.count();

        assertThatThrownBy(() -> transactionService.createCredit(
                transactionRequest,
                account.getId(),
                categoryExpense.getId(),
                user
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit transactions require an INCOME category.");

        Account updatedAccount =
                accountRepository.findById(account.getId())
                                .orElseThrow();

        assertThat(updatedAccount.getBalance())
                .as("The account balance should remain unchanged")
                .isEqualByComparingTo(new BigDecimal("900.00"));

        assertThat(transactionRepository.count())
                .as("No new transaction should be saved to the database")
                .isEqualTo(transactionBefore);
    }

    @Test
    @DisplayName("Should reject transaction when category is inactive")
    void shouldRejectTransactionWhenCategoryIsInactive() {
        TransactionRequest transactionRequest = new TransactionRequest(
                new BigDecimal("90.75"),
                "salary",
                LocalDateTime.now(),
                PaymentMethod.PIX
        );

        Long transactionBefore = transactionRepository.count();

        categoryExpense.archive();

        categoryRepository.save(categoryExpense);

        assertThatThrownBy(() ->transactionService.createDebit(
                transactionRequest,
                account.getId(),
                categoryExpense.getId(),
                user
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot use an inactive category.");

        Account updatedAccount = accountRepository
                .findById(account.getId())
                .orElseThrow();

        assertThat(updatedAccount.getBalance())
                .as("The account balance should remain unchanged")
                .isEqualByComparingTo(new BigDecimal("900.00"));

        assertThat(transactionRepository.count())
                .as("No new transaction should be saved to the database")
                .isEqualTo(transactionBefore);
    }

    @Test
    @DisplayName("Should rollback balance and not persist transaction in PostgreSQL when repository fails during debit")
    void shouldRollbackWhenDatabaseFailsOnCreateDebit() {

        Long accountId = account.getId();
        Long categoryId = categoryExpense.getId();

        long totalTransactionsBefore =
                transactionRepository.count();

        doThrow(new RuntimeException(
                "Simulated PostgreSQL connection failure"
        ))
                .when(transactionRepository)
                .save(any(Transaction.class));

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"),
                "food",
                LocalDateTime.now(),
                PaymentMethod.PIX
        );

        assertThatThrownBy(() ->
                transactionService.createDebit(
                        request,
                        accountId,
                        categoryId,
                        user
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated PostgreSQL connection failure");

        Account reloadedAccount =
                accountRepository.findById(accountId)
                        .orElseThrow();

        assertThat(reloadedAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("900.00"));

        assertThat(transactionRepository.count())
                .isEqualTo(totalTransactionsBefore);
    }

    @Test
    @DisplayName("Should rollback balance and not persist transaction in PostgreSQL when repository fails during credit")
    void shouldRollbackWhenDatabaseFailsOnCreateCredit() {

        Long accountId = account.getId();
        Long categoryId = categoryIncome.getId();

        long totalTransactionsBefore =
                transactionRepository.count();

        doThrow(new RuntimeException(
                "Simulated PostgreSQL connection failure"
        ))
                .when(transactionRepository)
                .save(any(Transaction.class));

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"),
                "salary",
                LocalDateTime.now(),
                PaymentMethod.PIX
        );

        assertThatThrownBy(() ->
                transactionService.createCredit(
                        request,
                        accountId,
                        categoryId,
                        user
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated PostgreSQL connection failure");

        Account reloadedAccount =
                accountRepository.findById(accountId)
                        .orElseThrow();

        assertThat(reloadedAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("900.00"));

        assertThat(transactionRepository.count())
                .isEqualTo(totalTransactionsBefore);
    }

    @Test
    @DisplayName("Should rollback balance and transaction status in PostgreSQL when repository fails during cancellation")
    void shouldRollbackWhenDatabaseFailsOnCancelTransaction() {

        Long transactionId = transaction.getId();
        Long accountId = account.getId();

        doThrow(new RuntimeException(
                "Simulated PostgreSQL connection failure"
        ))
                .when(transactionRepository)
                .save(any(Transaction.class));

        assertThatThrownBy(() ->
                transactionService.cancelTransaction(
                        transactionId,
                        user
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated PostgreSQL connection failure");

        Account reloadedAccount =
                accountRepository.findById(accountId)
                        .orElseThrow();

        Transaction reloadedTransaction =
                transactionRepository.findById(transactionId)
                        .orElseThrow();

        assertThat(reloadedAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("900.00"));

        assertThat(reloadedTransaction.getTransactionStatus())
                .isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should create credit and update account balance")
    void shouldCreateCreditAndUpdateAccountBalance() {

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("500.00"),
                "Salary",
                LocalDateTime.now(),
                PaymentMethod.PIX
        );

        TransactionResponse response =
                transactionService.createCredit(
                        request,
                        account.getId(),
                        categoryIncome.getId(),
                        user
                );

        Account updatedAccount =
                accountRepository.findById(account.getId())
                        .orElseThrow();

        assertThat(response).isNotNull();

        assertThat(updatedAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("1400.00"));

        assertThat(transactionRepository.count())
                .isEqualTo(2);

        Transaction persistedTransaction = transactionRepository.findById(response.id())
                .orElseThrow(() -> new AssertionError("The transaction was not found in the database"));

        assertThat(persistedTransaction.getTransactionType())
                .as("The transaction type should be CREDIT")
                .isEqualTo(TransactionType.CREDIT);

        assertThat(persistedTransaction.getTransactionStatus())
                .as("The transaction status should be COMPLETED")
                .isEqualTo(TransactionStatus.COMPLETED);

        assertThat(persistedTransaction.getAmount())
                .as("The transaction amount should be exactly 500.00")
                .isEqualByComparingTo(new BigDecimal("500.00"));

        assertThat(persistedTransaction.getAccount().getId())
                .as("The transaction should be linked to the correct account")
                .isEqualTo(account.getId());

        assertThat(persistedTransaction.getCategory().getId())
                .as("The transaction should be linked to the correct income category")
                .isEqualTo(categoryIncome.getId());

    }

    @Test
    @DisplayName("Should cancel debit and restore account balance")
    void shouldCancelDebitAndRestoreAccountBalance() {

        TransactionResponse response =
                transactionService.cancelTransaction(
                        transaction.getId(),
                        user
                );

        Account updatedAccount =
                accountRepository.findById(account.getId())
                        .orElseThrow();

        assertThat(response).isNotNull();

        assertThat(updatedAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("1000.00"));

        Transaction cancelledTransaction =
                transactionRepository.findById(transaction.getId())
                        .orElseThrow();

        assertThat(cancelledTransaction.getTransactionStatus())
                .isEqualTo(TransactionStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should cancel credit and restore account balance")
    void shouldCancelCreditAndRestoreAccountBalance() {

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"),
                "Credit to cancel",
                LocalDateTime.now(),
                PaymentMethod.PIX
        );

        TransactionResponse createdTransaction =
                transactionService.createCredit(
                        request,
                        account.getId(),
                        categoryIncome.getId(),
                        user
                );

        Account accountAfterCredit =
                accountRepository.findById(account.getId())
                        .orElseThrow();

        assertThat(accountAfterCredit.getBalance())
                .isEqualByComparingTo(new BigDecimal("1100.00"));

        transactionService.cancelTransaction(
                createdTransaction.id(),
                user
        );

        Account accountAfterCancellation =
                accountRepository.findById(account.getId())
                        .orElseThrow();

        assertThat(accountAfterCancellation.getBalance())
                .isEqualByComparingTo(new BigDecimal("900.00"));

        Transaction cancelledTransaction =
                transactionRepository.findById(createdTransaction.id())
                        .orElseThrow();

        assertThat(cancelledTransaction.getTransactionStatus())
                .isEqualTo(TransactionStatus.CANCELLED);
    }
}