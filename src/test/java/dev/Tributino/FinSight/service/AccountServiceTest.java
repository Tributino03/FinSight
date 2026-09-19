package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.account.AccountResponse;
import dev.Tributino.FinSight.mapper.AccountMapper;
import dev.Tributino.FinSight.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Ownership Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserService userService;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private User owner;
    private User attacker;
    private Account ownerAccount;

    @BeforeEach
    void setUp() {
        owner = mock(User.class);
        attacker = mock(User.class);
        ownerAccount = mock(Account.class);

        lenient().when(owner.getId()).thenReturn(1L);
        lenient().when(attacker.getId()).thenReturn(2L);
        lenient().when(ownerAccount.getUser()).thenReturn(owner);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return account when user is the owner")
        void shouldReturnAccountWhenUserIsOwner() {
            AccountResponse response = mock(AccountResponse.class);

            when(accountRepository.findById(10L)).thenReturn(Optional.of(ownerAccount));
            when(accountMapper.toResponse(ownerAccount)).thenReturn(response);

            AccountResponse result = accountService.findById(10L, owner);

            assertThat(result).isNotNull().isEqualTo(response);
            verify(accountRepository).findById(10L);
            verify(accountMapper).toResponse(ownerAccount);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when account belongs to another user")
        void shouldThrowAccessDeniedExceptionWhenAccountBelongsToAnotherUser() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(ownerAccount));

            assertThatThrownBy(() -> accountService.findById(10L, attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This account does not belong to you.");

            verify(accountMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("should return accounts when listing own accounts")
        void shouldReturnAccountsWhenListingOwnAccounts() {
            AccountResponse response = mock(AccountResponse.class);

            when(accountRepository.findByUserId(1L)).thenReturn(List.of(ownerAccount));
            when(accountMapper.toResponse(ownerAccount)).thenReturn(response);

            List<AccountResponse> result = accountService.findByUserId(1L, owner);

            assertThat(result).hasSize(1).contains(response);
            verify(accountRepository).findByUserId(1L);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when listing accounts from another user")
        void shouldThrowAccessDeniedExceptionWhenListingAccountsFromAnotherUser() {
            assertThatThrownBy(() -> accountService.findByUserId(1L, attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: You can only list your own accounts.");

            verify(accountRepository, never()).findByUserId(anyLong());
        }
    }

    @Nested
    @DisplayName("updateName")
    class UpdateName {

        @Test
        @DisplayName("should update account when user is the owner")
        void shouldUpdateAccountWhenUserIsOwner() {
            String newName = "Investment Account";
            AccountResponse response = mock(AccountResponse.class);

            when(accountRepository.findById(10L)).thenReturn(Optional.of(ownerAccount));
            when(accountRepository.save(ownerAccount)).thenReturn(ownerAccount);
            when(accountMapper.toResponse(ownerAccount)).thenReturn(response);

            AccountResponse result = accountService.updateName(10L, newName, owner);

            assertThat(result).isEqualTo(response);
            verify(ownerAccount).updateName(newName);
            verify(accountRepository).save(ownerAccount);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when updating account of another user")
        void shouldThrowAccessDeniedExceptionWhenUpdatingAnotherUserAccount() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(ownerAccount));

            assertThatThrownBy(() -> accountService.updateName(10L, "Hacked Name", attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This account does not belong to you.");

            verify(ownerAccount, never()).updateName(anyString());
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete account when user is the owner")
        void shouldDeleteAccountWhenUserIsOwner() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(ownerAccount));

            accountService.delete(10L, owner);

            verify(accountRepository).delete(ownerAccount);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when deleting account of another user")
        void shouldThrowAccessDeniedExceptionWhenDeletingAnotherUserAccount() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(ownerAccount));

            assertThatThrownBy(() -> accountService.delete(10L, attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This account does not belong to you.");

            verify(accountRepository, never()).delete(any());
        }
    }
}