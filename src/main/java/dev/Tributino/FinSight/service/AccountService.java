package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.account.AccountRequest;
import dev.Tributino.FinSight.dto.account.AccountResponse;
import dev.Tributino.FinSight.mapper.AccountMapper;
import dev.Tributino.FinSight.repository.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository, UserService userService, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.userService = userService;
        this.accountMapper = accountMapper;
    }

    public List<AccountResponse> findAll() {
        return accountRepository.findAll()
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    public AccountResponse findById(Long id){
        Account account = findEntityById(id);

        return accountMapper.toResponse(account);
    }

    Account findEntityById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    public List<AccountResponse> findByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    public AccountResponse create(AccountRequest accountRequest, Long userId) {
        User user = userService.findById(userId);

        Account newAccount = new Account(
                accountRequest.name(),
                accountRequest.accountType(),
                accountRequest.balance(),
                user
        );

        Account savedAccount = accountRepository.save(newAccount);

        return accountMapper.toResponse(savedAccount);
    }

    public AccountResponse updateName(Long id, String newName) {
        Account existingAccount = findEntityById(id);
        existingAccount.setName(newName);

        Account updatedAccount = accountRepository.save(existingAccount);
        return accountMapper.toResponse(updatedAccount);
    }

    public void delete(Long id) {
        Account account = findEntityById(id);
        accountRepository.delete(account);
    }
}