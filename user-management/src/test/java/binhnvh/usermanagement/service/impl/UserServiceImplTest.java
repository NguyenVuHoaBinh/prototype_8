package binhnvh.usermanagement.service.impl;

import binhnvh.usermanagement.dto.UserDto;
import binhnvh.usermanagement.entity.Role;
import binhnvh.usermanagement.entity.User;
import binhnvh.usermanagement.exception.ResourceNotFoundException;
import binhnvh.usermanagement.repository.RoleRepository;
import binhnvh.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Set up roles
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");

        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setLocked(false);
        testUser.setCreatedAt(LocalDateTime.now().minusDays(10));
        testUser.setUpdatedAt(LocalDateTime.now().minusDays(5));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {
        @Test
        @DisplayName("Should return user by ID when user exists")
        void getUserById_shouldReturnUser_whenUserExists() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // Act
            UserDto result = userService.getUserById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getRoles()).contains("USER");

            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when user does not exist")
        void getUserById_shouldThrowException_whenUserDoesNotExist() {
            // Arrange
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id : '99'");

            verify(userRepository).findById(99L);
        }
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {
        @Test
        @DisplayName("Should create user with valid data")
        void createUser_shouldReturnCreatedUser_whenInputIsValid() {
            // Arrange
            UserDto userDto = UserDto.builder()
                    .username("newuser")
                    .email("new@example.com")
                    .password("password123")
                    .firstName("New")
                    .lastName("User")
                    .roles(Set.of("USER"))
                    .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

            User savedUser = new User();
            savedUser.setId(2L);
            savedUser.setUsername("newuser");
            savedUser.setEmail("new@example.com");
            savedUser.setPassword("encodedPassword");
            savedUser.setFirstName("New");
            savedUser.setLastName("User");
            savedUser.setEnabled(true);
            savedUser.setLocked(false);
            savedUser.setRoles(Set.of(userRole));
            savedUser.setCreatedAt(LocalDateTime.now());
            savedUser.setUpdatedAt(LocalDateTime.now());

            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // Act
            UserDto result = userService.createUser(userDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getRoles()).contains("USER");

            verify(userRepository).existsByUsername("newuser");
            verify(userRepository).existsByEmail("new@example.com");
            verify(passwordEncoder).encode("password123");
            verify(roleRepository).findByName("USER");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should set default enabled status when not provided")
        void createUser_shouldSetDefaultEnabledStatus_whenNotProvided() {
            // Arrange
            UserDto userDto = UserDto.builder()
                    .username("newuser")
                    .email("new@example.com")
                    .password("password123")
                    .firstName("New")
                    .lastName("User")
                    .roles(Set.of("USER"))
                    // No enabled field
                    .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
                User savedUser = userCaptor.getValue();
                savedUser.setId(2L);
                return savedUser;
            });

            // Act
            userService.createUser(userDto);

            // Assert
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.isEnabled()).isTrue(); // Default is true
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {
        @Test
        @DisplayName("Should update user with valid data")
        void updateUser_shouldReturnUpdatedUser_whenInputIsValid() {
            // Arrange
            UserDto userDto = UserDto.builder()
                    .username("updateduser")
                    .email("updated@example.com")
                    .firstName("Updated")
                    .lastName("User")
                    .roles(Set.of("USER", "ADMIN"))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsername("updateduser")).thenReturn(false);
            when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

            User updatedUser = new User();
            updatedUser.setId(1L);
            updatedUser.setUsername("updateduser");
            updatedUser.setEmail("updated@example.com");
            updatedUser.setPassword("encodedPassword");
            updatedUser.setFirstName("Updated");
            updatedUser.setLastName("User");
            updatedUser.setEnabled(true);
            updatedUser.setLocked(false);
            updatedUser.setRoles(Set.of(userRole, adminRole));
            updatedUser.setCreatedAt(testUser.getCreatedAt());
            updatedUser.setUpdatedAt(LocalDateTime.now());

            when(userRepository.save(any(User.class))).thenReturn(updatedUser);

            // Act
            UserDto result = userService.updateUser(1L, userDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("updateduser");
            assertThat(result.getEmail()).isEqualTo("updated@example.com");
            assertThat(result.getFirstName()).isEqualTo("Updated");
            assertThat(result.getLastName()).isEqualTo("User");
            assertThat(result.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");

            verify(userRepository).findById(1L);
            verify(userRepository).existsByUsername("updateduser");
            verify(userRepository).existsByEmail("updated@example.com");
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    class RoleManagementTests {
        @Test
        @DisplayName("Should add role to user")
        void addRoleToUser_shouldAddRoleToUser() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            UserDto result = userService.addRoleToUser(1L, "ADMIN");

            // Assert
            assertThat(result.getRoles()).hasSize(2);
            assertThat(result.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");

            verify(userRepository).findById(1L);
            verify(roleRepository).findByName("ADMIN");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should prevent removing last role")
        void removeRoleFromUser_shouldThrowException_whenRemovingLastRole() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

            // Act & Assert
            assertThatThrownBy(() -> userService.removeRoleFromUser(1L, "USER"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Cannot remove the only role from a user");

            verify(userRepository).findById(1L);
            verify(roleRepository).findByName("USER");
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Test
    @DisplayName("Should change password when current password is correct")
    void changePassword_shouldUpdatePassword_whenCurrentPasswordIsCorrect() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        // Use the userCaptor to capture the user being saved
        when(userRepository.save(userCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // Act
        UserDto result = userService.changePassword(1L, "currentPassword", "newPassword");

        // Assert
        assertThat(result).isNotNull();
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("newEncodedPassword");

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("currentPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }
}