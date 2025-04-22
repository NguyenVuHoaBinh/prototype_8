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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public AuthResponse authenticate(AuthRequest loginRequest) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set the authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = jwtProvider.generateToken(authentication);
        LocalDateTime expirationDate = jwtProvider.getExpirationDateFromToken(jwt);

        // Get user details
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseGet(() -> userRepository.findByEmail(loginRequest.getUsername())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "username or email", loginRequest.getUsername())));

        // Update last login time
        userRepository.updateLastLoginTime(user.getId(), LocalDateTime.now());

        // Extract roles from authentication
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5)) // Remove "ROLE_" prefix
                .collect(Collectors.toList());

        // Build and return the authentication response
        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .expiresAt(expirationDate)
                .build();
    }

    @Override
    @Transactional
    public UserDto register(UserDto userDto) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + userDto.getUsername());
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + userDto.getEmail());
        }

        // Create new user entity
        User user = User.builder()
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .enabled(true)
                .locked(false)
                .roles(new HashSet<>())
                .build();

        // Assign default role if none specified
        Set<String> roleNames = userDto.getRoles();
        if (roleNames == null || roleNames.isEmpty()) {
            roleNames = Set.of("USER");
        }

        // Fetch and assign roles
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
            user.addRole(role);
        }

        // Save the user
        User savedUser = userRepository.save(user);

        // Convert to DTO for response
        return UserDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .enabled(savedUser.isEnabled())
                .locked(savedUser.isLocked())
                .roles(savedUser.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .lastLoginAt(savedUser.getLastLoginAt())
                .build();
    }

    /**
     * Validate a JWT token.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    @Override
    public boolean validateToken(String token) {
        return jwtProvider.validateToken(token);
    }
}