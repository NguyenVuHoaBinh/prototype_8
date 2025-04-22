package binhnvh.usermanagement;

import binhnvh.usermanagement.dto.AuthRequest;
import binhnvh.usermanagement.dto.AuthResponse;
import binhnvh.usermanagement.dto.UserDto;
import binhnvh.usermanagement.entity.Role;
import binhnvh.usermanagement.entity.User;
import binhnvh.usermanagement.repository.RoleRepository;
import binhnvh.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserManagementIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private User testUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";

        // Create roles if they don't exist
        adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ADMIN");
                    role.setDescription("Administrator role");
                    return roleRepository.save(role);
                });

        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    role.setDescription("Regular user role");
                    return roleRepository.save(role);
                });

        // Create a test user
        testUser = new User();
        testUser.setUsername("integration_test");
        testUser.setEmail("integration_test@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setFirstName("Integration");
        testUser.setLastName("Test");
        testUser.setEnabled(true);
        testUser.setLocked(false);

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);

        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        userRepository.delete(testUser);
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        // Arrange
        AuthRequest request = new AuthRequest("integration_test", "password");
        HttpEntity<AuthRequest> entity = new HttpEntity<>(request);

        // Act
        ResponseEntity<AuthResponse> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                entity,
                AuthResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getAccessToken());
        assertEquals("Bearer", response.getBody().getTokenType());
        assertEquals("integration_test", response.getBody().getUsername());
        assertEquals("integration_test@example.com", response.getBody().getEmail());
        assertTrue(response.getBody().getRoles().contains("USER"));
    }

    @Test
    void register_shouldCreateNewUser_whenInputIsValid() {
        // Arrange
        UserDto request = UserDto.builder()
                .username("new_integration_user")
                .email("new_integration@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .roles(Collections.singleton("USER"))
                .build();

        HttpEntity<UserDto> entity = new HttpEntity<>(request);

        // Act
        ResponseEntity<UserDto> response = restTemplate.exchange(
                baseUrl + "/auth/register",
                HttpMethod.POST,
                entity,
                UserDto.class
        );

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("new_integration_user", response.getBody().getUsername());
        assertEquals("new_integration@example.com", response.getBody().getEmail());
        assertEquals("New", response.getBody().getFirstName());
        assertEquals("User", response.getBody().getLastName());
        assertTrue(response.getBody().getEnabled());
        assertFalse(response.getBody().getLocked());
        assertTrue(response.getBody().getRoles().contains("USER"));

        // Clean up
        userRepository.findByUsername("new_integration_user")
                .ifPresent(userRepository::delete);
    }

    @Test
    void getUserById_shouldReturnUser_whenAuthenticated() {
        // Arrange - First login to get token
        AuthRequest loginRequest = new AuthRequest("integration_test", "password");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginRequest,
                AuthResponse.class
        );

        String token = loginResponse.getBody().getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<UserDto> response = restTemplate.exchange(
                baseUrl + "/users/" + testUser.getId(),
                HttpMethod.GET,
                entity,
                UserDto.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUser.getId(), response.getBody().getId());
        assertEquals(testUser.getUsername(), response.getBody().getUsername());
        assertEquals(testUser.getEmail(), response.getBody().getEmail());
    }

    @Test
    void getUserById_shouldReturnUnauthorized_whenNotAuthenticated() {
        // Act
        ResponseEntity<UserDto> response = restTemplate.getForEntity(
                baseUrl + "/users/" + testUser.getId(),
                UserDto.class
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}