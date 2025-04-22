package binhnvh.usermanagement.service;

import binhnvh.usermanagement.dto.AuthRequest;
import binhnvh.usermanagement.dto.AuthResponse;
import binhnvh.usermanagement.dto.UserDto;

public interface AuthService {

    AuthResponse authenticate(AuthRequest loginRequest);

    UserDto register(UserDto userDto);

    boolean validateToken(String token);
}