package dev.Tributino.FinSight.dto.account;

import dev.Tributino.FinSight.enums.AccountType;

import java.math.BigDecimal;

public record AccountRequest(
        String name,
        AccountType accountType,
        BigDecimal balance
) {
}
