package dev.Tributino.FinSight.dto.transaction;

import dev.Tributino.FinSight.enums.PaymentMethod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount must have at most 17 integer digits and 2 decimal places"
        )
        BigDecimal amount,

        @NotBlank(message = "Description is required")
        @Size(min = 5, max = 255, message = "Description must be between 5 and 255 characters")
        String description,

        @NotNull(message = "Transaction date is required")
        LocalDateTime transactionDate,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod

) {
}