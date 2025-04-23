package binhnvh.usermanagement.repository;

import binhnvh.usermanagement.entity.Role;
import binhnvh.usermanagement.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Clean up from previous tests
        userRepository.deleteAll();

        // Create role
        userRole = new Role();
        userRole.setName("TEST_ROLE");
        userRole.setDescription("Test role for integration tests");
        userRole = roleRepository.save(userRole);

        // Create test user
        testUser = new User();
        testUser.setUsername("integrationtest");
        testUser.setEmail("integration@example.com");
        testUser.setPassword("password123");
        testUser.setFirstName("Integration");
        testUser.setLastName("Test");
        testUser.setEnabled(true);
        testUser.setLocked(false);
        testUser.addRole(userRole);

        testUser = userRepository.save(testUser);

        // Clear persistence context to ensure we're getting from database
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should find user by username")
    void findByUsername_shouldReturnUser_whenUsernameExists() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("integrationtest");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("integrationtest");
        assertThat(foundUser.get().getEmail()).isEqualTo("integration@example.com");
        assertThat(foundUser.get().getRoles()).hasSize(1);
        assertThat(foundUser.get().getRoles().iterator().next().getName()).isEqualTo("TEST_ROLE");
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_shouldReturnUser_whenEmailExists() {
        // Act
        Optional<User> foundUser = userRepository.findByEmail("integration@example.com");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("integrationtest");
    }

    @Test
    @DisplayName("Should return empty when username doesn't exist")
    void findByUsername_shouldReturnEmpty_whenUsernameDoesNotExist() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("nonexistentuser");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should check if username exists")
    void existsByUsername_shouldReturnTrue_whenUsernameExists() {
        // Act
        boolean exists = userRepository.existsByUsername("integrationtest");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should update last login time")
    void updateLastLoginTime_shouldUpdateTime() {
        // Arrange
        LocalDateTime loginTime = LocalDateTime.now();

        // Act
        int updatedRows = userRepository.updateLastLoginTime(testUser.getId(), loginTime);

        // Fetch fresh user from database
        User updatedUser = entityManager.find(User.class, testUser.getId());

        // Assert
        assertThat(updatedRows).isEqualTo(1);
        assertThat(updatedUser.getLastLoginAt()).isNotNull();
        // Compare only up to seconds precision to avoid test failures due to millisecond differences
        assertThat(updatedUser.getLastLoginAt().toLocalDate()).isEqualTo(loginTime.toLocalDate());
        assertThat(updatedUser.getLastLoginAt().getHour()).isEqualTo(loginTime.getHour());
        assertThat(updatedUser.getLastLoginAt().getMinute()).isEqualTo(loginTime.getMinute());
    }
}