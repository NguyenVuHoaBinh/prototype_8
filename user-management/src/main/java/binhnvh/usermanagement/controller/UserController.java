package binhnvh.usermanagement.controller;

import binhnvh.usermanagement.dto.UserDto;
import binhnvh.usermanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDto>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        log.info("Getting all users with pagination: {}", pageable);
        Page<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        log.info("Getting user by ID: {}", id);
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUsername(#username)")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        log.info("Getting user by username: {}", username);
        UserDto user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUserEmail(#email)")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        log.info("Getting user by email: {}", email);
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        log.info("Creating new user with username: {}", userDto.getUsername());
        UserDto createdUser = userService.createUser(userDto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        log.info("Updating user with ID: {}", id);
        UserDto updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    public ResponseEntity<UserDto> changePassword(
            @PathVariable Long id,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        log.info("Changing password for user with ID: {}", id);
        UserDto updatedUser = userService.changePassword(id, currentPassword, newPassword);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> setUserEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        log.info("Setting enabled status to {} for user with ID: {}", enabled, id);
        UserDto updatedUser = userService.setUserEnabled(id, enabled);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> setUserLocked(@PathVariable Long id, @RequestParam boolean locked) {
        log.info("Setting locked status to {} for user with ID: {}", locked, id);
        UserDto updatedUser = userService.setUserLocked(id, locked);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> addRoleToUser(@PathVariable Long userId, @PathVariable String roleName) {
        log.info("Adding role {} to user with ID: {}", roleName, userId);
        UserDto updatedUser = userService.addRoleToUser(userId, roleName);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> removeRoleFromUser(@PathVariable Long userId, @PathVariable String roleName) {
        log.info("Removing role {} from user with ID: {}", roleName, userId);
        UserDto updatedUser = userService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.ok(updatedUser);
    }
}