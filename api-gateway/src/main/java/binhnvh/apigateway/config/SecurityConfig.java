package binhnvh.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for the API Gateway.
 *
 * This class configures security for the API Gateway, including:
 * - CORS configuration
 * - Path-based security rules
 * - Authentication requirements
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Defines the security filter chain for the API Gateway.
     *
     * @param http The ServerHttpSecurity to configure
     * @return The configured SecurityWebFilterChain
     */
    @Bean
    @Order(1)
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)  // Disable CSRF for API Gateway
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchange -> exchange
                        // Public endpoints
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers("/api/public/**").permitAll()

                        // Endpoints requiring authentication
                        .pathMatchers("/api/**").authenticated()

                        // Management endpoints - restrict to admin role
                        .pathMatchers("/actuator/**").hasRole("ADMIN")

                        // Any other request requires authentication
                        .anyExchange().authenticated()
                )
                // Configure basic authentication for development purposes
                // This would be replaced with JWT or OAuth2 in production
                .httpBasic(httpBasic -> {})
                .build();
    }

    /**
     * Configures CORS for the API Gateway.
     *
     * @return A CorsConfigurationSource with CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));  // In production, restrict to specific origins
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}