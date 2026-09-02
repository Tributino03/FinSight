package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.repository.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

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

    public List<Account> findByUser(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public Account create(Account accountData, Long userId) {
        User user = userService.findById(userId);

        Account newAccount = new Account(
                accountData.getName(),
                accountData.getAccountType(),
                accountData.getBalance(),
                user
        );

        return accountRepository.save(newAccount);
    }

    public Account updateName(Long id, String newName) {
        Account existingAccount = findById(id);

        existingAccount.setName(newName);

        return accountRepository.save(existingAccount);
    }

    public Account save(Account account) {
        return accountRepository.save(account);
    }

    public void delete(Long id) {
        Account account = findById(id);
        accountRepository.delete(account);
    }
}