package dev.Tributino.FinSight.controller;

import dev.Tributino.FinSight.domain.User;
import dev.Tributino.FinSight.dto.user.AdminUserResponse;
import dev.Tributino.FinSight.dto.user.UserRoleUpdateRequest;
import dev.Tributino.FinSight.dto.user.UserStatusUpdateRequest;
import dev.Tributino.FinSight.enums.Role;
import dev.Tributino.FinSight.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        Page<AdminUserResponse> users = adminService.listUsers(name, email, role, enabled, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> findById(@PathVariable Long id) {
        AdminUserResponse response = adminService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateStatus(
            @AuthenticationPrincipal User loggedUser,
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {

        AdminUserResponse response = adminService.updateStatus(loggedUser, id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> updateRole(
            @AuthenticationPrincipal User loggedUser,
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request) {

        AdminUserResponse response = adminService.updateRole(loggedUser, id, request);
        return ResponseEntity.ok(response);
    }
}