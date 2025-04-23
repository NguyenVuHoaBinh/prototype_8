package binhnvh.usermanagement.controller;

import binhnvh.usermanagement.dto.AuthRequest;
import binhnvh.usermanagement.dto.AuthResponse;
import binhnvh.usermanagement.dto.UserDto;
import binhnvh.usermanagement.security.JwtAuthenticationFilter;
import binhnvh.usermanagement.security.JwtProvider;
import binhnvh.usermanagement.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({AuthControllerApiTest.MockDependenciesConfig.class})
class AuthControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @TestConfiguration
    static class MockDependenciesConfig {

        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return Mockito.mock(JwtAuthenticationFilter.class);
        }

        @Bean
        JwtProvider jwtProvider() {
            return Mockito.mock(JwtProvider.class);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return Mockito.mock(UserDetailsService.class);
        }
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/login should return 200 with JWT token when credentials are valid")
    void login_shouldReturnOkWithToken_whenCredentialsAreValid() throws Exception {
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

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/register should return 201 when registration is successful")
    void register_shouldReturnCreated_whenRegistrationIsSuccessful() throws Exception {
        UserDto request = UserDto.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .roles(Set.of("USER"))
                .build();

        UserDto response = UserDto.builder()
                .id(1L)
                .username("newuser")
                .email("new@example.com")
                .firstName("New")
                .lastName("User")
                .enabled(true)
                .locked(false)
                .roles(Set.of("USER"))
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.register(any(UserDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /auth/validate should return true when token is valid")
    void validateToken_shouldReturnTrue_whenTokenIsValid() throws Exception {
        when(authService.validateToken("valid-token")).thenReturn(true);

        mockMvc.perform(get("/api/auth/validate")
                        .param("token", "valid-token")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /auth/validate should return false when token is invalid")
    void validateToken_shouldReturnFalse_whenTokenIsInvalid() throws Exception {
        when(authService.validateToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/auth/validate")
                        .param("token", "invalid-token")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }
}