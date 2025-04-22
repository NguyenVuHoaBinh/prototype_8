package binhnvh.usermanagement.service;

import binhnvh.usermanagement.dto.UserDto;
import binhnvh.usermanagement.entity.Role;
import binhnvh.usermanagement.entity.User;
import binhnvh.usermanagement.exception.ResourceNotFoundException;
import binhnvh.usermanagement.exception.UserAlreadyExistsException;
import binhnvh.usermanagement.repository.RoleRepository;
import binhnvh.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private binhnvh.usermanagement.service.UserServiceImpl userService;

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

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertTrue(result.getRoles().contains("USER"));

        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_shouldThrowException_whenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(99L);
        });

        verify(userRepository).findById(99L);
    }

    @Test
    void getUserByUsername_shouldReturnUser_whenUserExists() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.getUserByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_shouldThrowException_whenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserByUsername("nonexistent");
        });

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void getUserByEmail_shouldReturnUser_whenUserExists() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.getUserByEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getUserByEmail_shouldThrowException_whenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserByEmail("nonexistent@example.com");
        });

        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void getAllUsers_shouldReturnPageOfUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Collections.singletonList(testUser);
        Page<User> page = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<UserDto> result = userService.getAllUsers(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());

        verify(userRepository).findAll(pageable);
    }

    @Test
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
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
        assertTrue(result.getRoles().contains("USER"));

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(roleRepository).findByName("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowException_whenUsernameExists() {
        // Arrange
        UserDto userDto = UserDto.builder()
                .username("existinguser")
                .email("new@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.createUser(userDto);
        });

        verify(userRepository).existsByUsername("existinguser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowException_whenEmailExists() {
        // Arrange
        UserDto userDto = UserDto.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.createUser(userDto);
        });

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser_whenInputIsValid() {
        // Arrange
        UserDto userDto = UserDto.builder()
                .username("updateduser")
                .email("updated@example.com")
                .firstName("Updated")
                .lastName("User")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("updateduser")).thenReturn(false);
        when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("updateduser");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setPassword("encodedPassword");
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("User");
        updatedUser.setEnabled(true);
        updatedUser.setLocked(false);
        updatedUser.setRoles(Set.of(userRole));
        updatedUser.setCreatedAt(testUser.getCreatedAt());
        updatedUser.setUpdatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.updateUser(1L, userDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("updateduser", result.getUsername());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("Updated", result.getFirstName());
        assertEquals("User", result.getLastName());

        verify(userRepository).findById(1L);
        verify(userRepository).existsByUsername("updateduser");
        verify(userRepository).existsByEmail("updated@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_shouldNotCheckUniqueness_whenUsernameAndEmailUnchanged() {
        // Arrange
        UserDto userDto = UserDto.builder()
                .username("testuser")   // Same username
                .email("test@example.com")   // Same email
                .firstName("Updated")
                .lastName("User")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        updatedUser.setPassword("encodedPassword");
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("User");
        updatedUser.setEnabled(true);
        updatedUser.setLocked(false);
        updatedUser.setRoles(Set.of(userRole));

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.updateUser(1L, userDto);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Updated", result.getFirstName());

        verify(userRepository).findById(1L);
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_shouldThrowException_whenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUser(99L);
        });

        verify(userRepository).findById(99L);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void setUserEnabled_shouldUpdateEnabledStatus() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        updatedUser.setEnabled(false); // Changed to false
        updatedUser.setRoles(testUser.getRoles());

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.setUserEnabled(1L, false);

        // Assert
        assertNotNull(result);
        assertFalse(result.getEnabled());

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void setUserLocked_shouldUpdateLockedStatus() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        updatedUser.setLocked(true); // Changed to true
        updatedUser.setRoles(testUser.getRoles());

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.setUserLocked(1L, true);

        // Assert
        assertNotNull(result);
        assertTrue(result.getLocked());

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void addRoleToUser_shouldAddRoleToUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        Set<Role> updatedRoles = new HashSet<>();
        updatedRoles.add(userRole);
        updatedRoles.add(adminRole);
        updatedUser.setRoles(updatedRoles);

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.addRoleToUser(1L, "ADMIN");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().contains("ADMIN"));
        assertTrue(result.getRoles().contains("USER"));

        verify(userRepository).findById(1L);
        verify(roleRepository).findByName("ADMIN");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void removeRoleFromUser_shouldRemoveRoleFromUser() {
        // Arrange
        // First, add ADMIN role to test user
        testUser.getRoles().add(adminRole);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        Set<Role> updatedRoles = new HashSet<>();
        updatedRoles.add(userRole); // ADMIN role removed
        updatedUser.setRoles(updatedRoles);

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.removeRoleFromUser(1L, "ADMIN");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRoles().contains("USER"));
        assertFalse(result.getRoles().contains("ADMIN"));

        verify(userRepository).findById(1L);
        verify(roleRepository).findByName("ADMIN");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void removeRoleFromUser_shouldThrowException_whenRemovingLastRole() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            userService.removeRoleFromUser(1L, "USER");
        });

        verify(userRepository).findById(1L);
        verify(roleRepository).findByName("USER");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_shouldUpdatePassword_whenCurrentPasswordIsCorrect() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        updatedUser.setPassword("newEncodedPassword");
        updatedUser.setRoles(testUser.getRoles());

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.changePassword(1L, "currentPassword", "newPassword");

        // Assert
        assertNotNull(result);

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("currentPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_shouldThrowException_whenCurrentPasswordIsIncorrect() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            userService.changePassword(1L, "wrongPassword", "newPassword");
        });

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}