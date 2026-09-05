package dev.Tributino.FinSight.dto.transaction;

import dev.Tributino.FinSight.enums.PaymentMethod;
import dev.Tributino.FinSight.enums.TransactionStatus;
import dev.Tributino.FinSight.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        String description,
        TransactionType transactionType,
        LocalDateTime transactionDate,
        PaymentMethod paymentMethod,
        TransactionStatus transactionStatus
) {
}
