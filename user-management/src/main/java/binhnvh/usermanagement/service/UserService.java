package binhnvh.usermanagement.service;

import binhnvh.usermanagement.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserDto getUserById(Long id);

    UserDto getUserByUsername(String username);

    UserDto getUserByEmail(String email);

    Page<UserDto> getAllUsers(Pageable pageable);

    UserDto createUser(UserDto userDto);

    UserDto updateUser(Long id, UserDto userDto);

    void deleteUser(Long id);

    UserDto setUserEnabled(Long id, boolean enabled);

    UserDto setUserLocked(Long id, boolean locked);

    UserDto addRoleToUser(Long userId, String roleName);

    UserDto removeRoleFromUser(Long userId, String roleName);

    UserDto changePassword(Long id, String currentPassword, String newPassword);
}