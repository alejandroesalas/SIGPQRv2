package com.sigpqr.user.controller;

import com.sigpqr.common.dto.ApiResponse;
import com.sigpqr.common.dto.PageResponse;
import com.sigpqr.common.enums.Profile;
import com.sigpqr.user.dto.ProfileResponseDto;
import com.sigpqr.user.dto.RegisterStudentDto;
import com.sigpqr.user.dto.RegisterTeacherDto;
import com.sigpqr.user.dto.UpdateUserDto;
import com.sigpqr.user.dto.UserCountDto;
import com.sigpqr.user.dto.UserResponseDto;
import com.sigpqr.user.service.ProfileService;
import com.sigpqr.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;

    public UserController(UserService userService, ProfileService profileService) {
        this.userService = userService;
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "List users", description = "List users with optional profile filter and pagination")
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDto>>> listUsers(
            @RequestParam(required = false) Profile profile,
            Pageable pageable) {
        var page = userService.listUsers(profile, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PostMapping
    @Operation(summary = "Register student", description = "Public student registration")
    public ResponseEntity<ApiResponse<UserResponseDto>> registerStudent(
            @Valid @RequestBody RegisterStudentDto dto) {
        var user = userService.registerStudent(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Student registered successfully", user));
    }

    @PostMapping("/teachers")
    @Operation(summary = "Create teacher", description = "Create a teacher (admin only)")
    public ResponseEntity<ApiResponse<UserResponseDto>> createTeacher(
            @Valid @RequestBody RegisterTeacherDto dto) {
        var user = userService.createTeacher(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Teacher created successfully", user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user", description = "Get user details by ID")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update user details")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", userService.updateUser(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Soft delete a user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }

    @PutMapping("/{id}/promote")
    @Operation(summary = "Promote user", description = "Promote a teacher to coordinator")
    public ResponseEntity<ApiResponse<UserResponseDto>> promoteUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("User promoted to coordinator", userService.promoteToCoordinator(id)));
    }

    @PutMapping("/{id}/demote")
    @Operation(summary = "Demote user", description = "Demote a coordinator to teacher")
    public ResponseEntity<ApiResponse<UserResponseDto>> demoteUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("User demoted to teacher", userService.demoteToTeacher(id)));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore user", description = "Restore a soft-deleted user")
    public ResponseEntity<ApiResponse<UserResponseDto>> restoreUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("User restored successfully", userService.restoreUser(id)));
    }

    @GetMapping("/profiles")
    @Operation(summary = "List profiles", description = "List all available user profiles")
    public ResponseEntity<ApiResponse<List<ProfileResponseDto>>> listProfiles() {
        return ResponseEntity.ok(ApiResponse.ok(profileService.listProfiles()));
    }

    @GetMapping("/count")
    @Operation(summary = "Count users", description = "Count users grouped by profile")
    public ResponseEntity<ApiResponse<List<UserCountDto>>> countUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.countByProfile()));
    }
}
