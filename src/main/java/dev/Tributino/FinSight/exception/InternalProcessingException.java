package dev.Tributino.FinSight.exception;

public class InternalProcessingException extends RuntimeException {
    public InternalProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}