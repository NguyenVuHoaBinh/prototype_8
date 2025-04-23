package binhnvh.usermanagement.service;

import binhnvh.usermanagement.dto.UserDto;
import binhnvh.usermanagement.entity.Role;
import binhnvh.usermanagement.entity.User;
import binhnvh.usermanagement.exception.ResourceNotFoundException;
import binhnvh.usermanagement.exception.UserAlreadyExistsException;
import binhnvh.usermanagement.repository.RoleRepository;
import binhnvh.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role userRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean database
        userRepository.deleteAll();

        // Create roles if they don't exist
        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    role.setDescription("Regular user role");
                    return roleRepository.save(role);
                });

        // Create test user
        testUser = new User();
        testUser.setUsername("servicetest");
        testUser.setEmail("service@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Service");
        testUser.setLastName("Test");
        testUser.setEnabled(true);
        testUser.setLocked(false);
        testUser.addRole(userRole);

        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should get user by ID")
    void getUserById_shouldReturnUser_whenUserExists() {
        // Act
        UserDto userDto = userService.getUserById(testUser.getId());

        // Assert
        assertThat(userDto).isNotNull();
        assertThat(userDto.getId()).isEqualTo(testUser.getId());
        assertThat(userDto.getUsername()).isEqualTo("servicetest");
        assertThat(userDto.getEmail()).isEqualTo("service@example.com");
        assertThat(userDto.getRoles()).contains("USER");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getUserById_shouldThrowException_whenUserDoesNotExist() {
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id : '999'");
    }

    @Test
    @DisplayName("Should create user with valid data")
    void createUser_shouldCreateAndReturnUser_whenInputIsValid() {
        // Arrange
        UserDto userDto = UserDto.builder()
                .username("newserviceuser")
                .email("newservice@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .roles(Set.of("USER"))
                .build();

        // Act
        UserDto createdUser = userService.createUser(userDto);

        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo("newserviceuser");
        assertThat(createdUser.getEmail()).isEqualTo("newservice@example.com");

        // Verify it was actually saved to the database
        assertThat(userRepository.findById(createdUser.getId())).isPresent();
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void createUser_shouldThrowException_whenUsernameExists() {
        // Arrange
        UserDto userDto = UserDto.builder()
                .username("servicetest") // Same as test user
                .email("different@example.com")
                .password("password123")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(userDto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    @DisplayName("Should get all users with pagination")
    void getAllUsers_shouldReturnPageOfUsers() {
        // Arrange - Create a few more users
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setUsername("paginationtest" + i);
            user.setEmail("pagination" + i + "@example.com");
            user.setPassword(passwordEncoder.encode("password"));
            user.setEnabled(true);
            user.addRole(userRole);
            userRepository.save(user);
        }

        // Act
        Page<UserDto> usersPage = userService.getAllUsers(PageRequest.of(0, 3));

        // Assert
        assertThat(usersPage).isNotNull();
        assertThat(usersPage.getContent()).isNotEmpty();
        assertThat(usersPage.getTotalElements()).isGreaterThanOrEqualTo(6); // Test user + 5 pagination users
        assertThat(usersPage.getSize()).isEqualTo(3); // Page size
    }
}