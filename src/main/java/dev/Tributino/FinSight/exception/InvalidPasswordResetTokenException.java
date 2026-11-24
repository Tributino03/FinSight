package dev.Tributino.FinSight.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("Invalid or expired password reset token");
    }
}