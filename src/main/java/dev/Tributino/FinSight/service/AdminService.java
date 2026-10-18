
package dev.Tributino.FinSight.service;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.AdminUserResponse;
import dev.Tributino.FinSight.dto.user.UserRoleUpdateRequest;
import dev.Tributino.FinSight.dto.user.UserStatusUpdateRequest;
import dev.Tributino.FinSight.enums.Role;
import dev.Tributino.FinSight.mapper.AdminMapper;
import dev.Tributino.FinSight.repository.AdminOperationLockRepository;
import dev.Tributino.FinSight.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AdminService {

    private static final long MIN_ACTIVE_ADMINS = 2L;
    private static final Long ADMIN_OPERATION_LOCK_ID = 1L;

    private final UserRepository userRepository;
    private final AdminMapper adminMapper;
    private final AdminOperationLockRepository adminOperationLockRepository;

    public AdminService(
            UserRepository userRepository,
            AdminMapper adminMapper,
            AdminOperationLockRepository adminOperationLockRepository
    ) {
        this.userRepository = userRepository;
        this.adminMapper = adminMapper;
        this.adminOperationLockRepository = adminOperationLockRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(
            String name,
            String email,
            Role role,
            Boolean enabled,
            Pageable pageable
    ) {
        return userRepository.findByFilters(
                normalizeFilter(name),
                normalizeFilter(email),
                role,
                enabled,
                pageable
        ).map(adminMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse findById(Long id) {
        return adminMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public AdminUserResponse updateStatus(
            User loggedUser,
            Long id,
            UserStatusUpdateRequest request
    ) {
        lockAdminOperations();

        User targetUser = findEntityById(id);

        validateNotSelf(loggedUser, targetUser);
        validateStatusChange(targetUser, request);

        applyStatusChange(targetUser, request.enabled());

        return adminMapper.toResponse(targetUser);
    }

    @Transactional
    public AdminUserResponse updateRole(
            User loggedUser,
            Long id,
            UserRoleUpdateRequest request
    ) {
        lockAdminOperations();

        User targetUser = findEntityById(id);

        validateNotSelf(loggedUser, targetUser);
        validateRoleChange(targetUser, request);

        applyRoleChange(targetUser, request.role());

        return adminMapper.toResponse(targetUser);
    }

    private User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found with id: " + id
                        )
                );
    }

    private void validateNotSelf(User loggedUser, User targetUser) {
        if (loggedUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException(
                    "Administrators cannot modify their own account through this endpoint."
            );
        }
    }

    private void validateStatusChange(
            User targetUser,
            UserStatusUpdateRequest request
    ) {
        if (targetUser.isEnabled() == request.enabled()) {
            return;
        }

        if (isActiveAdminBeingDisabled(targetUser, request.enabled())) {
            ensureMinimumActiveAdmins();
        }
    }

    private void validateRoleChange(
            User targetUser,
            UserRoleUpdateRequest request
    ) {
        if (targetUser.getRole() == request.role()) {
            return;
        }

        if (!targetUser.isEnabled() && request.role() == Role.ADMIN) {
            throw new IllegalArgumentException(
                    "A disabled user cannot be promoted to ADMIN."
            );
        }

        if (isActiveAdminBeingDemoted(targetUser, request.role())) {
            ensureMinimumActiveAdmins();
        }
    }

    private boolean isActiveAdminBeingDisabled(
            User targetUser,
            boolean requestedEnabled
    ) {
        return targetUser.getRole() == Role.ADMIN
                && targetUser.isEnabled()
                && !requestedEnabled;
    }

    private boolean isActiveAdminBeingDemoted(
            User targetUser,
            Role requestedRole
    ) {
        return targetUser.getRole() == Role.ADMIN
                && targetUser.isEnabled()
                && requestedRole == Role.USER;
    }

    private void ensureMinimumActiveAdmins() {
        long activeAdmins =
                userRepository.countByRoleAndEnabledTrue(Role.ADMIN);

        if (activeAdmins <= MIN_ACTIVE_ADMINS) {
            throw new IllegalArgumentException(
                    "At least two active administrators must remain. "
                            + "Current active administrators: " + activeAdmins
            );
        }
    }

    private void applyStatusChange(
            User targetUser,
            boolean requestedEnabled
    ) {
        if (targetUser.isEnabled() == requestedEnabled) {
            return;
        }

        if (requestedEnabled) {
            targetUser.enable();
        } else {
            targetUser.disable();
        }
    }

    private void applyRoleChange(User targetUser, Role role) {
        if (targetUser.getRole() == role) {
            return;
        }

        targetUser.changeRole(role);
    }

    private void lockAdminOperations() {
        adminOperationLockRepository
                .findByIdForUpdate(ADMIN_OPERATION_LOCK_ID)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Admin operation lock row not found."
                        )
                );
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
