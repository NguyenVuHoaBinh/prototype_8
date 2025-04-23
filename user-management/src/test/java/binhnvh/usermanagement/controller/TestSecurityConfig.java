package binhnvh.usermanagement.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF for tests (we'll add it manually)
                .authorizeHttpRequests(authorize -> authorize
                        // Allow auth endpoints without authentication
                        .requestMatchers("/api/auth/**").permitAll()
                        // Require authentication for all other endpoints
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}