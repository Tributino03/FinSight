package dev.Tributino.FinSight.mapper;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.dto.account.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getAccountType(),
                account.getBalance()
        );
    }
}
