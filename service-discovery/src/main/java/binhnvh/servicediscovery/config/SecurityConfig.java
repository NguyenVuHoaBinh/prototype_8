package binhnvh.servicediscovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for the Eureka Server.
 * Secures the Eureka dashboard while allowing service registration and discovery.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // disable CSRF since Eureka clients don't send cookies
                .csrf(csrf -> csrf.disable())
                // enable CORS using the bean defined below
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Allow Eureka clients to register without authentication
                        .requestMatchers("/eureka/**").permitAll()
                        // Allow health and info endpoints for monitoring
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Require authentication for the dashboard and all other endpoints
                        .anyRequest().authenticated()
                )
                // use HTTP Basic in the non‑deprecated style
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Configure CORS for the Eureka Server.
     * Uses the servlet-based CorsConfigurationSource.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
