package binhnvh.usermanagement.security;

import binhnvh.usermanagement.entity.User;
import binhnvh.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final UserRepository userRepository;

    public boolean isCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        return user.getUsername().equals(currentUsername) || user.getEmail().equals(currentUsername);
    }

    public boolean isCurrentUsername(String username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        return username.equals(currentUsername);
    }

    public boolean isCurrentUserEmail(String email) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Optional<User> userOptional = userRepository.findByUsername(currentUsername);

        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(currentUsername);

            if (userOptional.isEmpty()) {
                return false;
            }
        }

        User user = userOptional.get();
        return user.getEmail().equals(email);
    }
}