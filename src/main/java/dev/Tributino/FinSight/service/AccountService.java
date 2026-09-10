package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.account.AccountRequest;
import dev.Tributino.FinSight.dto.account.AccountResponse;
import dev.Tributino.FinSight.mapper.AccountMapper;
import dev.Tributino.FinSight.repository.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
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

    public AccountResponse findById(Long id, User loggedUser) {
        Account account = findEntityByIdAndUser(id, loggedUser);
        return accountMapper.toResponse(account);
    }

    Account findEntityByIdAndUser(Long id, User loggedUser) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(loggedUser.getId())) {
            throw new AccessDeniedException("Access denied: This account does not belong to you.");
        }
        return account;
    }

    public List<AccountResponse> findByUserId(Long userId, User loggedUser) {
        if (!userId.equals(loggedUser.getId())) {
            throw new AccessDeniedException("Access denied: You can only list your own accounts.");
        }

        return accountRepository.findByUserId(userId)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    public AccountResponse create(AccountRequest accountRequest, Long userId) {
        User user = userService.findEntityById(userId);

        Account newAccount = new Account(
                accountRequest.name(),
                accountRequest.accountType(),
                accountRequest.balance(),
                user
        );

        Account savedAccount = accountRepository.save(newAccount);

        return accountMapper.toResponse(savedAccount);
    }

    public AccountResponse updateName(Long id, String newName, User loggedUser) {
        Account existingAccount = findEntityByIdAndUser(id, loggedUser);

        existingAccount.setName(newName);
        Account updatedAccount = accountRepository.save(existingAccount);
        return accountMapper.toResponse(updatedAccount);
    }

    public void delete(Long id, User loggedUser) {
        Account account = findEntityByIdAndUser(id, loggedUser);
        accountRepository.delete(account);
    }
}