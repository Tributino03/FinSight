package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.repository.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    public AccountService(AccountRepository accountRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    public List<Account> findByUser(Long idUser) {
        User user = userService.findById(idUser);

        return accountRepository.findByUser(user);
    }

    public Account create(Account account, Long idUser) {
        User user = userService.findById(idUser);

        validateBalance(account.getBalance());

        Account newAccount = new Account(
                account.getName(),
                account.getAccountType(),
                account.getBalance(),
                user
        );

        return accountRepository.save(newAccount);
    }

    public Account update(Long id, Account accountData) {
        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        existingAccount.setName(accountData.getName());

        return accountRepository.save(existingAccount);
    }

    public void delete(Long id){
        Account account = findById(id);

        accountRepository.delete(account);
    }

    private void validateBalance(BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The account balance cannot be less than zero.");
        }
    }
}