package dev.Tributino.FinSight.dto.transaction;

import dev.Tributino.FinSight.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        String description,

        @NotNull(message = "Transaction date is required")
        LocalDateTime transactionDate,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod

) {
}