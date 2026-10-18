package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.AdminUserResponse;
import dev.Tributino.FinSight.dto.user.UserRoleUpdateRequest;
import dev.Tributino.FinSight.dto.user.UserStatusUpdateRequest;
import dev.Tributino.FinSight.enums.Role;
import dev.Tributino.FinSight.mapper.AdminMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import dev.Tributino.FinSight.domain.AdminOperationLock;
import dev.Tributino.FinSight.repository.AdminOperationLockRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private AdminOperationLockRepository adminOperationLockRepository;

    @InjectMocks
    private AdminService adminService;

    private User user;
    private AdminUserResponse mockResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        pageable = PageRequest.of(0, 10);

        mockResponse = new AdminUserResponse(
                1L,
                "Admin User",
                "admin@finsight.dev",
                Role.ADMIN,
                true,
                LocalDateTime.now()
        );

        lenient().when(user.getId()).thenReturn(1L);

        lenient().when(adminOperationLockRepository.findByIdForUpdate(anyLong()))
                .thenReturn(Optional.of(mock(AdminOperationLock.class)));
    }

    @Nested
    @DisplayName("listUsers Filters Tests")
    class ListUsersFiltersTests {

        @Test
        @DisplayName("Should list users with all filters provided correctly")
        void shouldListUsersWithAllFiltersProvided() {
            String name = "Lucas";
            String email = "lucas@finsight.dev";
            Role role = Role.ADMIN;
            Boolean enabled = true;

            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findByFilters(name, email, role, enabled, pageable))
                    .thenReturn(userPage);
            when(adminMapper.toResponse(user)).thenReturn(mockResponse);

            Page<AdminUserResponse> result = adminService.listUsers(name, email, role, enabled, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(mockResponse, result.getContent().get(0));

            verify(userRepository, times(1)).findByFilters(name, email, role, enabled, pageable);
            verify(adminMapper, times(1)).toResponse(user);
        }

        @Test
        @DisplayName("Should normalize empty or blank text filters to null")
        void shouldNormalizeEmptyOrBlankFiltersToNull() {
            String name = "   ";
            String email = "";
            Role role = Role.USER;
            Boolean enabled = false;

            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findByFilters(null, null, role, enabled, pageable))
                    .thenReturn(userPage);
            when(adminMapper.toResponse(user)).thenReturn(mockResponse);

            Page<AdminUserResponse> result = adminService.listUsers(name, email, role, enabled, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());

            verify(userRepository, times(1)).findByFilters(null, null, role, enabled, pageable);
        }

        @Test
        @DisplayName("Should list users when all filters are null")
        void shouldListUsersWhenAllFiltersAreNull() {
            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findByFilters(null, null, null, null, pageable))
                    .thenReturn(userPage);
            when(adminMapper.toResponse(user)).thenReturn(mockResponse);

            Page<AdminUserResponse> result = adminService.listUsers(null, null, null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());

            verify(userRepository, times(1)).findByFilters(null, null, null, null, pageable);
        }

        @Test
        @DisplayName("Should return an empty page when no users match the filters")
        void shouldReturnEmptyPageWhenNoUsersFound() {
            Page<User> emptyPage = Page.empty();

            when(userRepository.findByFilters(null, null, Role.ADMIN, true, pageable))
                    .thenReturn(emptyPage);

            Page<AdminUserResponse> result = adminService.listUsers(null, null, Role.ADMIN, true, pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());

            verify(userRepository, times(1)).findByFilters(null, null, Role.ADMIN, true, pageable);
            verifyNoInteractions(adminMapper);
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return user response when user exists")
        void shouldReturnUserResponseWhenUserExists() {
            Long userId = 1L;
            AdminUserResponse adminUserResponse = mock(AdminUserResponse.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(adminMapper.toResponse(user)).thenReturn(adminUserResponse);

            AdminUserResponse result = adminService.findById(userId);

            assertThat(result).isNotNull().isEqualTo(adminUserResponse);
            verify(userRepository).findById(userId);
            verify(adminMapper).toResponse(user);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when user does not exist")
        void shouldThrowEntityNotFoundExceptionWhenUserDoesNotExist() {
            Long userId = 5L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.findById(userId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found with id: " + userId);

            verify(userRepository).findById(userId);
            verify(adminMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("updateStatus Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when administrator tries to modify their own account")
        void shouldThrowIllegalArgumentExceptionWhenAdminTriesToModifySelf() {
            UserStatusUpdateRequest request = mock(UserStatusUpdateRequest.class);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> adminService.updateStatus(user, 1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Administrators cannot modify their own account through this endpoint.");

            verify(userRepository).findById(1L);
            verify(adminMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when target user does not exist")
        void shouldThrowEntityNotFoundExceptionWhenUserDoesNotExist() {
            Long targetUserId = 99L;
            UserStatusUpdateRequest request = mock(UserStatusUpdateRequest.class);
            when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateStatus(user, targetUserId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found with id: " + targetUserId);

            verify(userRepository).findById(targetUserId);
            verify(adminMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should return response without invoking state change when status is already equal")
        void shouldReturnResponseWithoutInvokingStateChangeWhenStatusIsAlreadyEqual() {
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            UserStatusUpdateRequest request = mock(UserStatusUpdateRequest.class);
            AdminUserResponse expectedResponse = mock(AdminUserResponse.class);

            when(targetUser.getId()).thenReturn(targetUserId);
            when(targetUser.isEnabled()).thenReturn(true);
            when(request.enabled()).thenReturn(true);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(adminMapper.toResponse(targetUser)).thenReturn(expectedResponse);

            AdminUserResponse result = adminService.updateStatus(user, targetUserId, request);

            assertThat(result).isNotNull().isEqualTo(expectedResponse);

            verify(targetUser, never()).enable();
            verify(targetUser, never()).disable();
            verify(userRepository, never()).countByRoleAndEnabledTrue(any());
            verify(adminMapper, times(1)).toResponse(targetUser);
        }

        @Test
        @DisplayName("Should enable inactive user successfully when requested")
        void shouldEnableInactiveUserSuccessfullyWhenRequested() {
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            UserStatusUpdateRequest request = mock(UserStatusUpdateRequest.class);
            AdminUserResponse expectedResponse = mock(AdminUserResponse.class);

            when(targetUser.getId()).thenReturn(targetUserId);
            when(targetUser.isEnabled()).thenReturn(false);
            when(request.enabled()).thenReturn(true);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(adminMapper.toResponse(targetUser)).thenReturn(expectedResponse);

            AdminUserResponse result = adminService.updateStatus(user, targetUserId, request);

            assertThat(result).isNotNull().isEqualTo(expectedResponse);

            verify(targetUser, times(1)).enable();
            verify(targetUser, never()).disable();
            verify(userRepository, never()).countByRoleAndEnabledTrue(any());
            verify(adminMapper, times(1)).toResponse(targetUser);
        }

        @Test
        @DisplayName("Should disable active admin successfully when more than minimum active admins remain")
        void shouldDisableActiveAdminSuccessfullyWhenSufficientAdminsRemain() {
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            UserStatusUpdateRequest request = mock(UserStatusUpdateRequest.class);
            AdminUserResponse expectedResponse = mock(AdminUserResponse.class);
            long activeAdmins = 3L;

            when(targetUser.getId()).thenReturn(targetUserId);
            when(targetUser.getRole()).thenReturn(Role.ADMIN);
            when(targetUser.isEnabled()).thenReturn(true);
            when(request.enabled()).thenReturn(false);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(activeAdmins);
            when(adminMapper.toResponse(targetUser)).thenReturn(expectedResponse);

            AdminUserResponse result = adminService.updateStatus(user, targetUserId, request);

            assertThat(result).isNotNull().isEqualTo(expectedResponse);

            verify(userRepository, times(1)).countByRoleAndEnabledTrue(Role.ADMIN);
            verify(targetUser, times(1)).disable();
            verify(adminMapper, times(1)).toResponse(targetUser);
        }

        @Test
        @DisplayName("Should disable active regular user without checking minimum admin count")
        void shouldDisableActiveRegularUserWithoutCheckingMinimumAdmins() {
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            UserStatusUpdateRequest request = mock(UserStatusUpdateRequest.class);
            AdminUserResponse expectedResponse = mock(AdminUserResponse.class);

            when(targetUser.getId()).thenReturn(targetUserId);
            when(targetUser.getRole()).thenReturn(Role.USER);
            when(targetUser.isEnabled()).thenReturn(true);
            when(request.enabled()).thenReturn(false);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(adminMapper.toResponse(targetUser)).thenReturn(expectedResponse);

            AdminUserResponse result = adminService.updateStatus(user, targetUserId, request);

            assertThat(result).isNotNull().isEqualTo(expectedResponse);

            verify(targetUser, times(1)).disable();
            verify(userRepository, never()).countByRoleAndEnabledTrue(any());
            verify(adminMapper, times(1)).toResponse(targetUser);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when attempting to disable an admin with fewer than two active admins remaining")
        void shouldThrowIllegalArgumentExceptionWhenDisablingAdminWithFewerThanMinimumActiveAdmins() {
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            UserStatusUpdateRequest request = mock(UserStatusUpdateRequest.class);
            long activeAdmins = 2L;

            when(targetUser.getId()).thenReturn(targetUserId);
            when(targetUser.getRole()).thenReturn(Role.ADMIN);
            when(targetUser.isEnabled()).thenReturn(true);
            when(request.enabled()).thenReturn(false);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(activeAdmins);

            assertThatThrownBy(() -> adminService.updateStatus(user, targetUserId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least two active administrators must remain. Current active administrators: " + activeAdmins);

            verify(userRepository, times(1)).countByRoleAndEnabledTrue(Role.ADMIN);
            verify(targetUser, never()).disable();
            verify(adminMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("updateRole Tests")
    class UpdateRoleTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when administrator tries to modify their own role")
        void shouldThrowIllegalArgumentExceptionWhenAdminTriesToModifyOwnRole(){
            UserRoleUpdateRequest request = mock(UserRoleUpdateRequest.class);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> adminService.updateRole(user, 1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Administrators cannot modify their own account through this endpoint.");

            verify(userRepository).findById(1L);
            verify(adminMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when target user does not exist for role update")
        void shouldThrowEntityNotFoundExceptionWhenUserDoesNotExistForRoleUpdate(){
            UserRoleUpdateRequest request = mock(UserRoleUpdateRequest.class);
            Long targetUserId = 99L;

            when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateRole(user, targetUserId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found with id: " + targetUserId);

            verify(userRepository).findById(targetUserId);
             verify(adminMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should return response without invoking role change when role is already equal")
        void shouldReturnResponseWithoutInvokingRoleChangeWhenRoleIsAlreadyEqual(){
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            UserRoleUpdateRequest request = mock(UserRoleUpdateRequest.class);
            AdminUserResponse response = mock(AdminUserResponse.class);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(targetUser.getRole()).thenReturn(Role.USER);
            when(request.role()).thenReturn(Role.USER);
            when(adminMapper.toResponse(targetUser)).thenReturn(response);

            AdminUserResponse result = adminService.updateRole(user, targetUserId, request);

            assertThat(result).isNotNull().isEqualTo(response);

            verify(targetUser, never()).changeRole(any());
            verify(userRepository, never()).countByRoleAndEnabledTrue(any());
            verify(adminMapper, times(1)).toResponse(targetUser);
        }

        @Test
        @DisplayName("Should change user role successfully when promoting or demoting safely")
        void shouldChangeUserRoleSuccessfullyWhenPromotingOrDemotingSafely(){
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            UserRoleUpdateRequest  request = mock(UserRoleUpdateRequest.class);
            AdminUserResponse response = mock(AdminUserResponse.class);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(targetUser.getRole()).thenReturn(Role.USER);
            when(targetUser.isEnabled()).thenReturn(true);
            when(adminMapper.toResponse(targetUser)).thenReturn(response);
            when(request.role()).thenReturn(Role.ADMIN);

            AdminUserResponse result = adminService.updateRole(user, targetUserId, request);

            assertThat(result).isNotNull().isEqualTo(response);

            verify(userRepository).findById(targetUserId);
            verify(targetUser, times(1)).changeRole(any());
            verify(userRepository, never()).countByRoleAndEnabledTrue(any());
            verify(adminMapper, times(1)).toResponse(targetUser);
        }

        @Test
        @DisplayName("Should demote active admin successfully when more than minimum active admins remain")
        void shouldDemoteActiveAdminSuccessfullyWhenSufficientAdminsRemain(){
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            Long activeAdmins = 3L;
            UserRoleUpdateRequest request = mock(UserRoleUpdateRequest.class);
            AdminUserResponse response = mock(AdminUserResponse.class);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(targetUser.getRole()).thenReturn(Role.ADMIN);
            when(targetUser.isEnabled()).thenReturn(true);
            when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(activeAdmins);
            when(request.role()).thenReturn(Role.USER);
            when(adminMapper.toResponse(targetUser)).thenReturn(response);

            AdminUserResponse result = adminService.updateRole(user, targetUserId, request);

            assertThat(result).isNotNull().isEqualTo(response);

            verify(userRepository).findById(targetUserId);
            verify(userRepository, times(1)).countByRoleAndEnabledTrue(any());
            verify(targetUser, times(1)).changeRole(any());
            verify(adminMapper).toResponse(targetUser);

        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when attempting to demote an admin with fewer than two active admins remaining")
        void shouldThrowIllegalArgumentExceptionWhenDemotingAdminWithFewerThanMinimumActiveAdmins(){
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            Long activeAdmins = 2L;
            UserRoleUpdateRequest request = mock(UserRoleUpdateRequest.class);
            AdminUserResponse response = mock(AdminUserResponse.class);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(targetUser.getRole()).thenReturn(Role.ADMIN);
            when(targetUser.isEnabled()).thenReturn(true);
            when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(activeAdmins);
            when(request.role()).thenReturn(Role.USER);

            assertThatThrownBy(() -> adminService.updateRole(user, targetUserId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least two active administrators must remain. "
                            + "Current active administrators: " + activeAdmins);

            verify(userRepository).findById(targetUserId);
            verify(userRepository, times(1)).countByRoleAndEnabledTrue(any());
            verify(targetUser, never()).changeRole(any());
            verify(adminMapper, never()).toResponse(targetUser);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when attempting to promote a disabled user to admin")
        void shouldThrowIllegalArgumentExceptionWhenPromotingDisabledUserToAdmin() {
            Long targetUserId = 2L;
            User targetUser = mock(User.class);
            Long activeAdmins = 2L;
            UserRoleUpdateRequest request = mock(UserRoleUpdateRequest.class);
            AdminUserResponse response = mock(AdminUserResponse.class);

            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(targetUser.getRole()).thenReturn(Role.USER);
            when(targetUser.isEnabled()).thenReturn(false);
            when(request.role()).thenReturn(Role.ADMIN);

            assertThatThrownBy(() -> adminService.updateRole(user, targetUserId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("A disabled user cannot be promoted to ADMIN.");

            verify(userRepository).findById(targetUserId);
            verify(userRepository, never()).countByRoleAndEnabledTrue(any());
            verify(targetUser, never()).changeRole(any());
            verify(adminMapper, never()).toResponse(targetUser);
        }
    }

}