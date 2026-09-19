package dev.Tributino.FinSight.domain;

import dev.Tributino.FinSight.enums.AccountType;
import dev.Tributino.FinSight.enums.CategoryType;
import dev.Tributino.FinSight.enums.PaymentMethod;
import dev.Tributino.FinSight.enums.TransactionStatus;
import dev.Tributino.FinSight.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private User defaultUser;
    private Account defaultAccount;
    private Category incomeCategory;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        defaultUser = new User();
        defaultAccount = new Account("Checking", AccountType.CHECKING, new BigDecimal("1000.00"), defaultUser);
        incomeCategory = new Category("Salary", CategoryType.INCOME, defaultUser);
        expenseCategory = new Category("Groceries", CategoryType.EXPENSE, defaultUser);
    }

    private Transaction createValidDebitTransaction() {
        return new Transaction(
                new BigDecimal("50.00"),
                "Market Purchase",
                TransactionType.DEBIT,
                PaymentMethod.DEBIT_CARD,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                defaultAccount,
                expenseCategory
        );
    }

    private Transaction createValidCreditTransaction() {
        return new Transaction(
                new BigDecimal("200.00"),
                "Freelance Payment",
                TransactionType.CREDIT,
                PaymentMethod.PIX,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                defaultAccount,
                incomeCategory
        );
    }

    @Nested
    @DisplayName("Amount Validation Tests")
    class AmountValidationTests {

        @Test
        @DisplayName("Should throw exception when transaction amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            null,
                            "Purchase",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            expenseCategory
                    )
            );

            assertEquals("The transaction amount must be greater than zero.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when transaction amount is zero or negative")
        void shouldThrowExceptionWhenAmountIsZeroOrNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            BigDecimal.ZERO,
                            "Purchase",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            expenseCategory
                    )
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("-10.00"),
                            "Purchase",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            expenseCategory
                    )
            );
        }
    }

    @Nested
    @DisplayName("Description Validation Tests")
    class DescriptionValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when description is null or blank in constructor")
        void shouldThrowExceptionWhenDescriptionIsInvalid(String invalidDescription) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            invalidDescription,
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            expenseCategory
                    )
            );

            assertEquals("The transaction description cannot be empty.", exception.getMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when mutating description to null or blank")
        void shouldThrowExceptionWhenMutatingDescriptionToInvalidValue(String invalidDescription) {
            Transaction transaction = createValidDebitTransaction();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transaction.setDescription(invalidDescription)
            );

            assertEquals("The transaction description cannot be empty.", exception.getMessage());
        }

        @Test
        @DisplayName("Should update description successfully")
        void shouldUpdateDescriptionSuccessfully() {
            Transaction transaction = createValidDebitTransaction();

            transaction.setDescription("Updated Description");

            assertEquals("Updated Description", transaction.getDescription());
        }
    }

    @Nested
    @DisplayName("Mandatory Field Validation Tests")
    class MandatoryFieldValidationTests {

        @Test
        @DisplayName("Should throw exception when transaction type is null")
        void shouldThrowExceptionWhenTransactionTypeIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Purchase",
                            null,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            expenseCategory
                    )
            );

            assertEquals("The transaction type is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when payment method is null")
        void shouldThrowExceptionWhenPaymentMethodIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Purchase",
                            TransactionType.DEBIT,
                            null,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            expenseCategory
                    )
            );

            assertEquals("The payment method is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when transaction status is null")
        void shouldThrowExceptionWhenTransactionStatusIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Purchase",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            null,
                            LocalDateTime.now(),
                            defaultAccount,
                            expenseCategory
                    )
            );

            assertEquals("The transaction status is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when transaction date is null")
        void shouldThrowExceptionWhenTransactionDateIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Purchase",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            null,
                            defaultAccount,
                            expenseCategory
                    )
            );

            assertEquals("The transaction date is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when setting transaction date to null")
        void shouldThrowExceptionWhenSettingTransactionDateToNull() {
            Transaction transaction = createValidDebitTransaction();

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transaction.setTransactionDate(null)
            );

            assertEquals("The transaction date is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when account is null")
        void shouldThrowExceptionWhenAccountIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Purchase",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            null,
                            expenseCategory
                    )
            );

            assertEquals("The transaction account is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when category is null")
        void shouldThrowExceptionWhenCategoryIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Purchase",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            null
                    )
            );

            assertEquals("The transaction category is required.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Business Rule & Compatibility Tests")
    class BusinessRuleTests {

        @Test
        @DisplayName("Should throw exception when DEBIT uses an INCOME category")
        void shouldThrowExceptionWhenDebitUsesIncomeCategory() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Invalid Debit",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            incomeCategory
                    )
            );

            assertEquals("Debit transactions require an EXPENSE category.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when CREDIT uses an EXPENSE category")
        void shouldThrowExceptionWhenCreditUsesExpenseCategory() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Invalid Credit",
                            TransactionType.CREDIT,
                            PaymentMethod.PIX,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            expenseCategory
                    )
            );

            assertEquals("Credit transactions require an INCOME category.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when category belongs to another user")
        void shouldThrowExceptionWhenCategoryBelongsToAnotherUser() {
            User otherUser = new User();
            Category otherUserCategory = new Category("Car", CategoryType.EXPENSE, otherUser);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Cross-user category",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            otherUserCategory
                    )
            );

            assertEquals("The custom category does not belong to the account owner.", exception.getMessage());
        }

        @Test
        @DisplayName("Should allow transaction with system category where user is null")
        void shouldAllowTransactionWithSystemCategory() {
            Category systemCategory = new Category("General", CategoryType.EXPENSE, null);

            Transaction transaction = new Transaction(
                    new BigDecimal("50.00"),
                    "Using system default",
                    TransactionType.DEBIT,
                    PaymentMethod.DEBIT_CARD,
                    TransactionStatus.COMPLETED,
                    LocalDateTime.now(),
                    defaultAccount,
                    systemCategory
            );

            assertEquals(systemCategory, transaction.getCategory());
        }

        @Test
        @DisplayName("Should throw exception when category is inactive")
        void shouldThrowExceptionWhenCategoryIsInactive() {
            expenseCategory.archive();

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> new Transaction(
                            new BigDecimal("50.00"),
                            "Inactive category use",
                            TransactionType.DEBIT,
                            PaymentMethod.DEBIT_CARD,
                            TransactionStatus.COMPLETED,
                            LocalDateTime.now(),
                            defaultAccount,
                            expenseCategory
                    )
            );

            assertEquals("Cannot use an inactive category.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Cancellation Lifecycle Tests")
    class CancellationTests {

        @Test
        @DisplayName("Should cancel a COMPLETED transaction successfully")
        void shouldCancelCompletedTransaction() {
            Transaction transaction = createValidDebitTransaction();

            transaction.cancel();

            assertEquals(TransactionStatus.CANCELLED, transaction.getTransactionStatus());
        }

        @Test
        @DisplayName("Should throw exception when cancelling an already cancelled transaction")
        void shouldThrowExceptionWhenCancellingAlreadyCancelledTransaction() {
            Transaction transaction = createValidDebitTransaction();
            transaction.cancel();

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    transaction::cancel
            );

            assertEquals("This transaction is already cancelled.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when cancelling transaction with status other than COMPLETED")
        void shouldThrowExceptionWhenCancellingNonCompletedTransaction() {
            Transaction transaction = new Transaction(
                    new BigDecimal("50.00"),
                    "Pending Transaction",
                    TransactionType.DEBIT,
                    PaymentMethod.DEBIT_CARD,
                    TransactionStatus.PENDING,
                    LocalDateTime.now(),
                    defaultAccount,
                    expenseCategory
            );

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    transaction::cancel
            );

            assertEquals("This transaction cannot be cancelled.", exception.getMessage());
        }
    }
}