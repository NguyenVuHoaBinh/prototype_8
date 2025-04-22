package binhnvh.usermanagement.controller;

import binhnvh.usermanagement.dto.AuthRequest;
import binhnvh.usermanagement.dto.AuthResponse;
import binhnvh.usermanagement.dto.UserDto;
import binhnvh.usermanagement.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.TestConfig.class)
class AuthControllerTest {

    @Configuration
    static class TestConfig {
        @Bean
        public AuthService authService() {
            return mock(AuthService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest("testuser", "password");
        AuthResponse response = AuthResponse.builder()
                .accessToken("test-token")
                .tokenType("Bearer")
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .roles(List.of("USER"))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void register_shouldReturnCreatedUser_whenInputIsValid() throws Exception {
        // Arrange
        UserDto request = UserDto.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .roles(Set.of("USER"))
                .build();

        UserDto response = UserDto.builder()
                .id(2L)
                .username("newuser")
                .email("newuser@example.com")
                .firstName("New")
                .lastName("User")
                .enabled(true)
                .locked(false)
                .roles(Set.of("USER"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(authService.register(any(UserDto.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @WithMockUser
    void validateToken_shouldReturnTrue_whenTokenIsValid() throws Exception {
        // Arrange
        when(authService.validateToken("valid-token")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/auth/validate")
                        .param("token", "valid-token")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser
    void validateToken_shouldReturnFalse_whenTokenIsInvalid() throws Exception {
        // Arrange
        when(authService.validateToken("invalid-token")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/auth/validate")
                        .param("token", "invalid-token")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }
}