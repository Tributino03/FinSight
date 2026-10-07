package dev.Tributino.FinSight.dto.account;

import dev.Tributino.FinSight.enums.AccountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AccountRequest(

        @NotBlank(message = "Account name is required")
        @Size(min = 5, max = 100)
        String name,

        @NotNull(message = "Account type is required")
        AccountType accountType,

        @NotNull(message = "Balance is required")
        @PositiveOrZero(message = "Balance cannot be negative")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Balance must have at most 17 integer digits and 2 decimal places"
        )
        BigDecimal balance

) {
}