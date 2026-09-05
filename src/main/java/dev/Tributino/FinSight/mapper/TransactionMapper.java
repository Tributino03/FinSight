package dev.Tributino.FinSight.mapper;

import dev.Tributino.FinSight.domain.Transaction;
import dev.Tributino.FinSight.dto.transaction.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction){
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionType(),
                transaction.getTransactionDate(),
                transaction.getPaymentMethod(),
                transaction.getTransactionStatus()
        );
    }
}
