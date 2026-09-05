package dev.Tributino.FinSight.dto.transaction;

import dev.Tributino.FinSight.domain.Transaction;
import dev.Tributino.FinSight.enums.PaymentMethod;
import dev.Tributino.FinSight.enums.TransactionStatus;
import dev.Tributino.FinSight.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(
        BigDecimal amount,
        String description,
        LocalDateTime transactionDate,
        PaymentMethod paymentMethod
) {
}
