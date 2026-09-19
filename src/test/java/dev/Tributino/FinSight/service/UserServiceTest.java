package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.RegisterRequest;
import dev.Tributino.FinSight.dto.user.UpdateUserRequest;
import dev.Tributino.FinSight.dto.user.UserResponse;
import dev.Tributino.FinSight.mapper.UserMapper;
import dev.Tributino.FinSight.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return UserResponse when user exists")
        void shouldReturnUserResponseWhenUserExists() {
            UserResponse response = mock(UserResponse.class);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            UserResponse result = userService.findById(1L);

            assertThat(result).isNotNull().isEqualTo(response);
            verify(userRepository).findById(1L);
            verify(userMapper).toResponse(user);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when user does not exist")
        void shouldThrowEntityNotFoundExceptionWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found");

            verify(userMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create and return UserResponse when email is available")
        void shouldCreateUserWhenEmailIsAvailable() {
            RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "plainPassword");
            UserResponse response = mock(UserResponse.class);

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(response);

            UserResponse result = userService.create(request);

            assertThat(result).isNotNull().isEqualTo(response);
            verify(userRepository).existsByEmail(request.email());
            verify(passwordEncoder).encode("plainPassword");
            verify(userRepository).save(any(User.class));
            verify(userMapper).toResponse(user);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when email is already registered")
        void shouldThrowIllegalArgumentExceptionWhenEmailAlreadyExists() {
            RegisterRequest request = new RegisterRequest("Jane Doe", "existing@example.com", "secret123");

            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("This email is already registered in the system.");

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any());
            verify(userMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update both name and password when both are provided")
        void shouldUpdateNameAndPasswordWhenProvided() {
            UpdateUserRequest request = new UpdateUserRequest("Updated Name", "newPassword");
            UserResponse response = mock(UserResponse.class);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPassword")).thenReturn("hashedNewPassword");
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(response);

            UserResponse result = userService.update(1L, request);

            assertThat(result).isNotNull().isEqualTo(response);
            verify(user).updateName("Updated Name");
            verify(user).changePassword("hashedNewPassword");
            verify(userRepository).save(user);
            verify(userMapper).toResponse(user);
        }

        @Test
        @DisplayName("should update only name when password is blank or null")
        void shouldUpdateOnlyNameWhenPasswordIsBlank() {
            UpdateUserRequest request = new UpdateUserRequest("Only Name Updated", "   ");
            UserResponse response = mock(UserResponse.class);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(response);

            UserResponse result = userService.update(1L, request);

            assertThat(result).isNotNull().isEqualTo(response);
            verify(user).updateName("Only Name Updated");
            verify(user, never()).changePassword(anyString());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when updating non-existent user")
        void shouldThrowEntityNotFoundExceptionWhenUpdatingNonExistentUser() {
            UpdateUserRequest request = new UpdateUserRequest("Name", "password");

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.update(99L, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete user when user exists")
        void shouldDeleteUserWhenExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            userService.delete(1L);

            verify(userRepository).findById(1L);
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when deleting non-existent user")
        void shouldThrowEntityNotFoundExceptionWhenDeletingNonExistentUser() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.delete(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository, never()).delete(any());
        }
    }
}