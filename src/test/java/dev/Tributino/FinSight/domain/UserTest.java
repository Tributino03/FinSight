package dev.Tributino.FinSight.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Nested
    @DisplayName("Constructor Validation Tests")
    class ConstructorValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when user name is invalid")
        void shouldThrowExceptionWhenNameIsInvalid(String invalidName) {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new User(invalidName, "gustavo@email.com", "secret123")
            );
            assertEquals("The user name cannot be empty.", ex.getMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when email is invalid")
        void shouldThrowExceptionWhenEmailIsInvalid(String invalidEmail) {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new User("Gustavo", invalidEmail, "secret123")
            );
            assertEquals("The user email is required.", ex.getMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception when password is invalid")
        void shouldThrowExceptionWhenPasswordIsInvalid(String invalidPassword) {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new User("Gustavo", "gustavo@email.com", invalidPassword)
            );
            assertEquals("The user password is required.", ex.getMessage());
        }

        @Test
        @DisplayName("Should create user successfully when all fields are valid")
        void shouldCreateUserSuccessfully() {
            User user = new User("Gustavo", "gustavo@email.com", "secret123");

            assertEquals("Gustavo", user.getName());
            assertEquals("gustavo@email.com", user.getEmail());
            assertEquals("secret123", user.getPassword());
            assertEquals("gustavo@email.com", user.getUsername());
        }
    }

    @Nested
    @DisplayName("Domain Methods Tests")
    class DomainMethodsTests {

        @Test
        @DisplayName("Should update name successfully")
        void shouldUpdateNameSuccessfully() {
            User user = new User("Gustavo", "gustavo@email.com", "secret123");

            user.updateName("Gustavo Tributino");

            assertEquals("Gustavo Tributino", user.getName());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Should throw exception when updating name to invalid value")
        void shouldThrowExceptionWhenUpdatingNameToInvalidValue(String invalidName) {
            User user = new User("Gustavo", "gustavo@email.com", "secret123");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> user.updateName(invalidName)
            );
            assertEquals("The user name cannot be empty.", ex.getMessage());
        }

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() {
            User user = new User("Gustavo", "gustavo@email.com", "secret123");

            user.changePassword("newSecret456");

            assertEquals("newSecret456", user.getPassword());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Should throw exception when changing password to invalid value")
        void shouldThrowExceptionWhenChangingPasswordToInvalidValue(String invalidPassword) {
            User user = new User("Gustavo", "gustavo@email.com", "secret123");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> user.changePassword(invalidPassword)
            );
            assertEquals("The user password is required.", ex.getMessage());
        }
    }
}