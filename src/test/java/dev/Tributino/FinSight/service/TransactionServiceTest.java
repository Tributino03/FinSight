package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.Account;
import dev.Tributino.FinSight.domain.Category;
import dev.Tributino.FinSight.domain.Transaction;
import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.transaction.TransactionRequest;
import dev.Tributino.FinSight.dto.transaction.TransactionResponse;
import dev.Tributino.FinSight.enums.CategoryType;
import dev.Tributino.FinSight.enums.PaymentMethod;
import dev.Tributino.FinSight.enums.TransactionType;
import dev.Tributino.FinSight.mapper.TransactionMapper;
import dev.Tributino.FinSight.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Ownership Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private User owner;
    private User attacker;
    private Account ownerAccount;
    private Category ownerCategory;
    private Transaction ownerTransaction;
    private TransactionRequest transactionRequest;
    private Category debitCategory;

    @BeforeEach
    void setUp() {
        owner = mock(User.class);
        attacker = mock(User.class);
        ownerAccount = mock(Account.class);
        ownerCategory = mock(Category.class);
        ownerTransaction = mock(Transaction.class);
        transactionRequest = mock(TransactionRequest.class);

        transactionRequest = new TransactionRequest(
                new BigDecimal("50.00"),
                "fitness",
                LocalDateTime.now(),
                PaymentMethod.DEBIT_CARD
        );

        lenient().when(owner.getId()).thenReturn(1L);
        lenient().when(attacker.getId()).thenReturn(2L);

        lenient().when(ownerAccount.getUser()).thenReturn(owner);
        lenient().when(ownerTransaction.getAccount()).thenReturn(ownerAccount);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return transaction when user is the account owner")
        void shouldReturnTransactionWhenUserIsOwner() {
            TransactionResponse response = mock(TransactionResponse.class);

            when(transactionRepository.findById(100L)).thenReturn(Optional.of(ownerTransaction));
            when(transactionMapper.toResponse(ownerTransaction)).thenReturn(response);

            TransactionResponse result = transactionService.findById(100L, owner);

            assertThat(result).isNotNull().isEqualTo(response);
            verify(transactionRepository).findById(100L);
            verify(transactionMapper).toResponse(ownerTransaction);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when transaction belongs to another user")
        void shouldThrowAccessDeniedExceptionWhenTransactionBelongsToAnotherUser() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(ownerTransaction));

            assertThatThrownBy(() -> transactionService.findById(100L, attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This transaction belongs to another user.");

            verify(transactionMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("findByAccountId")
    class FindByAccountId {

        @Test
        @DisplayName("should return transactions when account belongs to logged user")
        void shouldReturnTransactionsWhenAccountBelongsToUser() {
            TransactionResponse response = mock(TransactionResponse.class);

            when(accountService.findEntityByIdAndUser(10L, owner)).thenReturn(ownerAccount);
            when(transactionRepository.findByAccountId(10L)).thenReturn(List.of(ownerTransaction));
            when(transactionMapper.toResponse(ownerTransaction)).thenReturn(response);

            List<TransactionResponse> result = transactionService.findByAccountId(10L, owner);

            assertThat(result).hasSize(1).contains(response);
            verify(accountService).findEntityByIdAndUser(10L, owner);
            verify(transactionRepository).findByAccountId(10L);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when account belongs to another user")
        void shouldThrowAccessDeniedExceptionWhenAccountBelongsToAnotherUser() {
            when(accountService.findEntityByIdAndUser(10L, attacker))
                    .thenThrow(new AccessDeniedException("Access denied: This account does not belong to you."));

            assertThatThrownBy(() -> transactionService.findByAccountId(10L, attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This account does not belong to you.");

            verify(transactionRepository, never()).findByAccountId(anyLong());
        }
    }

    @Nested
    @DisplayName("findByCategoryId")
    class FindByCategoryId {

        @Test
        @DisplayName("should return transactions when category access is allowed")
        void shouldReturnTransactionsWhenCategoryBelongsToUser() {
            TransactionResponse response = mock(TransactionResponse.class);

            when(categoryService.findEntityByIdAndUser(5L, owner)).thenReturn(ownerCategory);
            when(transactionRepository.findByCategoryId(5L)).thenReturn(List.of(ownerTransaction));
            when(transactionMapper.toResponse(ownerTransaction)).thenReturn(response);

            List<TransactionResponse> result = transactionService.findByCategoryId(5L, owner);

            assertThat(result).hasSize(1).contains(response);
            verify(categoryService).findEntityByIdAndUser(5L, owner);
            verify(transactionRepository).findByCategoryId(5L);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when category belongs to another user")
        void shouldThrowAccessDeniedExceptionWhenCategoryBelongsToAnotherUser() {
            when(categoryService.findEntityByIdAndUser(5L, attacker))
                    .thenThrow(new AccessDeniedException("Access denied: This category belongs to another user."));

            assertThatThrownBy(() -> transactionService.findByCategoryId(5L, attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This category belongs to another user.");

            verify(transactionRepository, never()).findByCategoryId(anyLong());
        }
    }

    @Nested
    @DisplayName("createDebit")
    class CreateDebit {

        @Test
        @DisplayName("should throw RuntimeException when database fails while saving created debit")
        void shouldThrowRuntimeExceptionWhenDatabaseFailsOnCreateDebit() {
            Long accountId = 10L;
            Long categoryId = 2L;

            when(ownerCategory.getCategoryType()).thenReturn(dev.Tributino.FinSight.enums.CategoryType.EXPENSE);

            when(accountService.findEntityByIdAndUser(accountId, owner))
                    .thenReturn(ownerAccount);
            when(categoryService.findEntityByIdAndUser(categoryId, owner))
                    .thenReturn(ownerCategory);

            when(transactionRepository.save(any(Transaction.class)))
                    .thenThrow(new RuntimeException("Database connection timeout"));

            assertThatThrownBy(() -> transactionService.createDebit(
                    transactionRequest,
                    accountId,
                    categoryId,
                    owner
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection timeout");

            verify(accountService).findEntityByIdAndUser(accountId, owner);
            verify(categoryService).findEntityByIdAndUser(categoryId, owner);
            verify(transactionRepository).save(any(Transaction.class));
            verifyNoInteractions(transactionMapper);
        }
    }

    @Nested
    @DisplayName("createCredit")
    class CreateCredit {

        @Test
        @DisplayName("should throw RuntimeException when database fails while saving created credit")
        void shouldThrowRuntimeExceptionWhenDatabaseFailsOnCreateCredit() {

            Long accountId = 10L;
            Long categoryId = 2L;

            when(ownerCategory.getCategoryType()).thenReturn(CategoryType.INCOME);

            when(accountService.findEntityByIdAndUser(accountId, owner)).thenReturn(ownerAccount);
            when(categoryService.findEntityByIdAndUser(categoryId, owner)).thenReturn(ownerCategory);

            when(transactionRepository.save(any(Transaction.class)))
                    .thenThrow(new RuntimeException("Database connection timeout"));

            assertThatThrownBy(() ->transactionService.createCredit(
                    transactionRequest,
                    accountId,
                    categoryId,
                    owner
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection timeout");

            verify(accountService).findEntityByIdAndUser(accountId, owner);
            verify(categoryService).findEntityByIdAndUser(categoryId, owner);
            verify(transactionRepository).save(any(Transaction.class));
            verifyNoInteractions(transactionMapper);
        }
    }

    @Nested
    @DisplayName("cancelTransaction")
    class CancelTransaction {

        @Test
        @DisplayName("should cancel transaction when user is the account owner")
        void shouldCancelTransactionWhenUserIsOwner() {
            TransactionResponse response = mock(TransactionResponse.class);

            when(transactionRepository.findById(100L)).thenReturn(Optional.of(ownerTransaction));
            when(ownerTransaction.getTransactionType()).thenReturn(dev.Tributino.FinSight.enums.TransactionType.DEBIT);
            when(ownerTransaction.getAmount()).thenReturn(java.math.BigDecimal.valueOf(50.0));
            when(transactionRepository.save(ownerTransaction)).thenReturn(ownerTransaction);
            when(transactionMapper.toResponse(ownerTransaction)).thenReturn(response);

            TransactionResponse result = transactionService.cancelTransaction(100L, owner);

            assertThat(result).isEqualTo(response);
            verify(ownerTransaction).cancel();
            verify(ownerAccount).credit(java.math.BigDecimal.valueOf(50.0));
            verify(transactionRepository).save(ownerTransaction);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when attempting to cancel transaction of another user")
        void shouldThrowAccessDeniedExceptionWhenCancellingAnotherUserTransaction() {
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(ownerTransaction));

            assertThatThrownBy(() -> transactionService.cancelTransaction(100L, attacker))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied: This transaction belongs to another user.");

            verify(ownerTransaction, never()).cancel();
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw RuntimeException when database fails while saving cancelled transaction")
        void shouldThrowRuntimeExceptionWhenDatabaseFailsOnCancel() {

            when(transactionRepository.findById(100L)).thenReturn(Optional.of(ownerTransaction));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenThrow(new RuntimeException("Database connection timeout"));


            assertThatThrownBy(() -> transactionService.cancelTransaction(100L, owner))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection timeout");


            verify(transactionRepository).save(any(Transaction.class));
            verifyNoInteractions(transactionMapper);
        }
    }
}