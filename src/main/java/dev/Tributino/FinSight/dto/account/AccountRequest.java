package dev.Tributino.FinSight.dto.account;

import dev.Tributino.FinSight.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AccountRequest(

        @NotBlank(message = "Account name is required")
        String name,

        @NotNull(message = "Account type is required")
        AccountType accountType,

        @NotNull(message = "Balance is required")
        @PositiveOrZero(message = "Balance cannot be negative")
        BigDecimal balance

) {
}