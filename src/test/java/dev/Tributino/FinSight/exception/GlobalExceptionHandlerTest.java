package dev.Tributino.FinSight.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should return 404 when entity is not found")
    void shouldReturnNotFoundWhenEntityIsNotFound() {

        EntityNotFoundException exception =
                new EntityNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response =
                handler.handleEntityNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("User not found", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 400 when an illegal argument is provided")
    void shouldReturnBadRequestWhenIllegalArgumentIsProvided() {

        IllegalArgumentException exception =
                new IllegalArgumentException("The account name cannot be empty.");

        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals(
                "The account name cannot be empty.",
                response.getBody().message()
        );
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 409 when an illegal state occurs")
    void shouldReturnConflictWhenIllegalStateOccurs() {

        IllegalStateException exception =
                new IllegalStateException("This transaction is already cancelled.");

        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalState(exception);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals(
                "This transaction is already cancelled.",
                response.getBody().message()
        );
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 400 with validation message")
    void shouldReturnBadRequestWithValidationMessage() {

        Object target = new Object();

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "request");

        bindingResult.addError(
                new FieldError(
                        "request",
                        "name",
                        "Account name is required"
                )
        );

        MethodArgumentNotValidException exception =
                createValidationException(bindingResult);

        ResponseEntity<ErrorResponse> response =
                handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals(
                "Account name is required",
                response.getBody().message()
        );
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 403 when access is denied")
    void shouldReturnForbiddenWhenAccessIsDenied() {

        AccessDeniedException exception =
                new AccessDeniedException("Access denied.");

        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(exception);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().status());
        assertEquals("Access denied.", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 401 when credentials are invalid")
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() {

        BadCredentialsException exception =
                new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentials(exception);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().status());
        assertEquals(
                "Invalid email or password.",
                response.getBody().message()
        );
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 409 when data integrity is violated")
    void shouldReturnConflictWhenDataIntegrityIsViolated() {

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("Database constraint violation");

        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(exception);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals(
                "The request could not be completed because it conflicts with existing data.",
                response.getBody().message()
        );
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 400 when argument type does not match")
    void shouldReturnBadRequestWhenArgumentTypeDoesNotMatch() {

        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException(
                        "abc",
                        Long.class,
                        "id",
                        null,
                        new IllegalArgumentException("Invalid number")
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleTypeMismatch(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals(
                "Invalid value for parameter: id",
                response.getBody().message()
        );
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 400 when request body is malformed")
    void shouldReturnBadRequestWhenRequestBodyIsMalformed() {

        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("Malformed JSON", (HttpInputMessage) null);

        ResponseEntity<ErrorResponse> response =
                handler.handleMessageNotReadable(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals(
                "Malformed request body.",
                response.getBody().message()
        );
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 500 when an unexpected exception occurs")
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs() {

        Exception exception =
                new RuntimeException("Internal database error");

        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpectedException(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                response.getStatusCode().value()
        );
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals(
                "An unexpected error occurred.",
                response.getBody().message()
        );
        assertNotNull(response.getBody().timestamp());
    }

    private MethodArgumentNotValidException createValidationException(
            BeanPropertyBindingResult bindingResult
    ) {
        try {
            Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
            MethodParameter methodParameter =
                    new MethodParameter(method, 0);

            return new MethodArgumentNotValidException(
                    methodParameter,
                    bindingResult
            );

        } catch (NoSuchMethodException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static class TestController {

        @SuppressWarnings("unused")
        public void testMethod(String name) {
        }
    }
}