package binhnvh.usermanagement.service;

import binhnvh.usermanagement.dto.AuthRequest;
import binhnvh.usermanagement.dto.AuthResponse;
import binhnvh.usermanagement.dto.UserDto;
import binhnvh.usermanagement.entity.Role;
import binhnvh.usermanagement.entity.User;
import binhnvh.usermanagement.exception.ResourceNotFoundException;
import binhnvh.usermanagement.exception.UserAlreadyExistsException;
import binhnvh.usermanagement.repository.RoleRepository;
import binhnvh.usermanagement.repository.UserRepository;
import binhnvh.usermanagement.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private binhnvh.usermanagement.service.AuthServiceImpl authService;

    private User testUser;
    private Role userRole;
    private AuthRequest loginRequest;
    private UserDto registerRequest;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Set up test user
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
        userRole.setDescription("Regular user role");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setLocked(false);
        testUser.setRoles(new HashSet<>(Collections.singletonList(userRole)));

        // Set up login request
        loginRequest = new AuthRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        // Set up register request
        registerRequest = UserDto.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .roles(Set.of("USER"))
                .build();

        // Set up mock authentication
        Collection<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER"));
        authentication = new UsernamePasswordAuthenticationToken(
                "testuser", "password", authorities);
    }

    @Test
    void authenticate_shouldReturnAuthResponse_whenCredentialsAreValid() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateToken(any(Authentication.class))).thenReturn("test-token");
        when(jwtProvider.getExpirationDateFromToken(anyString()))
                .thenReturn(LocalDateTime.now().plusDays(1));

        // Act
        AuthResponse response = authService.authenticate(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());

        verify(userRepository).updateLastLoginTime(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void authenticate_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.authenticate(loginRequest);
        });
    }

    @Test
    void register_shouldReturnUserDto_whenRegistrationIsSuccessful() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(userRole));

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("new@example.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setFirstName("New");
        savedUser.setLastName("User");
        savedUser.setEnabled(true);
        savedUser.setLocked(false);
        savedUser.setRoles(new HashSet<>(Collections.singletonList(userRole)));
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserDto result = authService.register(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New", result.getFirstName());
        assertEquals("Last", result.getLastName());
        assertTrue(result.getEnabled());
        assertFalse(result.getLocked());
        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRoles().contains("USER"));

        verify(passwordEncoder).encode("password123");
        verify(roleRepository).findByName("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenUsernameExists() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenEmailExists() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenRoleNotFound() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void validateToken_shouldDelegateToJwtProvider() {
        // Arrange
        when(jwtProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtProvider.validateToken("invalid-token")).thenReturn(false);

        // Act & Assert
        assertTrue(authService.validateToken("valid-token"));
        assertFalse(authService.validateToken("invalid-token"));

        verify(jwtProvider, times(2)).validateToken(anyString());
    }
}