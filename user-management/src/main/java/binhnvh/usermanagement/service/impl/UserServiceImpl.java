package binhnvh.usermanagement.service;

import binhnvh.usermanagement.dto.UserDto;
import binhnvh.usermanagement.entity.Role;
import binhnvh.usermanagement.entity.User;
import binhnvh.usermanagement.exception.ResourceNotFoundException;
import binhnvh.usermanagement.exception.UserAlreadyExistsException;
import binhnvh.usermanagement.repository.RoleRepository;
import binhnvh.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapUserToDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return mapUserToDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapUserToDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapUserToDto);
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + userDto.getUsername());
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + userDto.getEmail());
        }

        // Create new user entity
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEnabled(userDto.getEnabled() != null ? userDto.getEnabled() : true);
        user.setLocked(userDto.getLocked() != null ? userDto.getLocked() : false);

        // Assign roles if specified
        if (userDto.getRoles() != null && !userDto.getRoles().isEmpty()) {
            for (String roleName : userDto.getRoles()) {
                Role role = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
                user.addRole(role);
            }
        } else {
            // Assign default USER role if none specified
            Role defaultRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "USER"));
            user.addRole(defaultRole);
        }

        // Save the user
        User savedUser = userRepository.save(user);
        return mapUserToDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Check username uniqueness if changed
        if (!user.getUsername().equals(userDto.getUsername()) &&
                userRepository.existsByUsername(userDto.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + userDto.getUsername());
        }

        // Check email uniqueness if changed
        if (!user.getEmail().equals(userDto.getEmail()) &&
                userRepository.existsByEmail(userDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + userDto.getEmail());
        }

        // Update user properties
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());

        if (userDto.getEnabled() != null) {
            user.setEnabled(userDto.getEnabled());
        }

        if (userDto.getLocked() != null) {
            user.setLocked(userDto.getLocked());
        }

        // Update roles if specified
        if (userDto.getRoles() != null) {
            // Clear existing roles
            user.getRoles().clear();

            // Add new roles
            for (String roleName : userDto.getRoles()) {
                Role role = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
                user.addRole(role);
            }
        }

        // Save and return the updated user
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserDto setUserEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setEnabled(enabled);
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }

    @Override
    @Transactional
    public UserDto setUserLocked(Long id, boolean locked) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setLocked(locked);
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }

    @Override
    @Transactional
    public UserDto addRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        user.addRole(role);
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }

    @Override
    @Transactional
    public UserDto removeRoleFromUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        // Check if this is the user's only role
        if (user.getRoles().size() <= 1 && user.getRoles().contains(role)) {
            throw new AccessDeniedException("Cannot remove the only role from a user");
        }

        user.removeRole(role);
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }

    @Override
    @Transactional
    public UserDto changePassword(Long id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Verify the current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AccessDeniedException("Current password is incorrect");
        }

        // Update the password
        user.setPassword(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(user);
        return mapUserToDto(updatedUser);
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .locked(user.isLocked())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}