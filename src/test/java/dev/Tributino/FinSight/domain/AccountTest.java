package dev.Tributino.FinSight.domain;

import dev.Tributino.FinSight.enums.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Account createAccountWithBalance(BigDecimal balance) {
        return new Account("Main Account", AccountType.CHECKING, balance, new User());
    }

    @Nested
    @DisplayName("Constructor Validation Tests")
    class ConstructorValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when account name is null or blank in constructor")
        void shouldThrowExceptionWhenNameIsInvalid(String invalidName) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Account(invalidName, AccountType.CHECKING, BigDecimal.TEN, new User())
            );

            assertEquals("The account name cannot be empty.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when account type is null")
        void shouldThrowExceptionWhenAccountTypeIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Account("Main Account", null, BigDecimal.TEN, new User())
            );

            assertEquals("The account type is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Account("Main Account", AccountType.CHECKING, BigDecimal.TEN, null)
            );

            assertEquals("The account user is required.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Initial Balance Tests")
    class InitialBalanceTests {

        @Test
        @DisplayName("Should throw exception when initial balance is null")
        void shouldThrowExceptionWhenInitialBalanceIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> createAccountWithBalance(null)
            );

            assertEquals("The account balance cannot be less than zero.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when initial balance is negative")
        void shouldThrowExceptionWhenInitialBalanceIsNegative() {
            BigDecimal negativeBalance = new BigDecimal("-0.01");

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> createAccountWithBalance(negativeBalance)
            );

            assertEquals("The account balance cannot be less than zero.", exception.getMessage());
        }

        @Test
        @DisplayName("Should create account when initial balance is zero")
        void shouldCreateAccountWhenInitialBalanceIsZero() {
            Account account = createAccountWithBalance(BigDecimal.ZERO);

            assertEquals(BigDecimal.ZERO, account.getBalance());
        }

        @Test
        @DisplayName("Should create account when initial balance is positive")
        void shouldCreateAccountWhenInitialBalanceIsPositive() {
            BigDecimal initialBalance = new BigDecimal("100.00");

            Account account = createAccountWithBalance(initialBalance);

            assertEquals(initialBalance, account.getBalance());
        }
    }

    @Nested
    @DisplayName("Credit Tests")
    class CreditTests {

        @Test
        @DisplayName("Should throw exception when credit amount is null")
        void shouldThrowExceptionWhenCreditAmountIsNull() {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> account.credit(null)
            );

            assertEquals("The amount must be greater than zero.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when credit amount is zero or negative")
        void shouldThrowExceptionWhenCreditAmountIsZeroOrNegative() {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            assertThrows(IllegalArgumentException.class, () -> account.credit(BigDecimal.ZERO));
            assertThrows(IllegalArgumentException.class, () -> account.credit(new BigDecimal("-50.00")));
        }

        @Test
        @DisplayName("Should increase balance when credit is successful")
        void shouldIncreaseBalanceWhenCreditIsSuccessful() {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            account.credit(new BigDecimal("50.00"));

            assertEquals(new BigDecimal("150.00"), account.getBalance());
        }
    }

    @Nested
    @DisplayName("Debit Tests")
    class DebitTests {

        @Test
        @DisplayName("Should throw exception when debit amount is null")
        void shouldThrowExceptionWhenDebitAmountIsNull() {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> account.debit(null)
            );

            assertEquals("The amount must be greater than zero.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when debit amount is zero or negative")
        void shouldThrowExceptionWhenDebitAmountIsZeroOrNegative() {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            assertThrows(IllegalArgumentException.class, () -> account.debit(BigDecimal.ZERO));
            assertThrows(IllegalArgumentException.class, () -> account.debit(new BigDecimal("-10.00")));
        }

        @Test
        @DisplayName("Should throw exception when debit is greater than available balance")
        void shouldThrowExceptionWhenDebitIsGreaterThanBalance() {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> account.debit(new BigDecimal("100.01"))
            );

            assertEquals("Insufficient balance.", exception.getMessage());
        }

        @Test
        @DisplayName("Should leave zero balance when debit equals current balance")
        void shouldAllowDebitWhenAmountIsEqualToBalance() {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            account.debit(new BigDecimal("100.00"));

            assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        }

        @Test
        @DisplayName("Should subtract amount from balance when debit is less than balance")
        void shouldSubtractBalanceWhenDebitIsLessThanBalance() {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            account.debit(new BigDecimal("40.00"));

            assertEquals(new BigDecimal("60.00"), account.getBalance());
        }
    }

    @Nested
    @DisplayName("Update Name Tests")
    class UpdateNameTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when updating name with null or blank value")
        void shouldThrowExceptionWhenUpdatingNameWithInvalidValue(String invalidName) {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> account.updateName(invalidName)
            );

            assertEquals("The account name cannot be empty.", exception.getMessage());
        }

        @Test
        @DisplayName("Should update account name successfully")
        void shouldUpdateAccountNameSuccessfully() {
            Account account = createAccountWithBalance(new BigDecimal("100.00"));

            account.updateName("Updated Emergency Fund");

            assertEquals("Updated Emergency Fund", account.getName());
        }
    }
}