package binhnvh.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Route configuration for the API Gateway.
 *
 * This class defines the routes that the API Gateway will use to forward requests to the appropriate microservices.
 * It also configures route-specific settings like rate limiting, request transformation, and circuit breaking.
 */
@Configuration
public class RouteConfig {

    /**
     * Configures the routes for the API Gateway.
     *
     * @param builder The RouteLocatorBuilder to use
     * @return A RouteLocator with the configured routes
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Tool Registry Service Route
                .route("tool-registry-service", r -> r
                        .path("/api/tools/**")
                        .filters(f -> f
                                .rewritePath("/api/tools/(?<segment>.*)", "/tool-registry/$1")
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver()))
                                .circuitBreaker(c -> c
                                        .setName("toolRegistryCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/tool-registry")))
                        .uri("lb://tool-registry-service"))  // Using service discovery

                // Flow Registry Service Route
                .route("flow-registry-service", r -> r
                        .path("/api/flows/**")
                        .filters(f -> f
                                .rewritePath("/api/flows/(?<segment>.*)", "/flow-registry/$1")
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver()))
                                .circuitBreaker(c -> c
                                        .setName("flowRegistryCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/flow-registry")))
                        .uri("lb://flow-registry-service"))  // Using service discovery

                // Execution Engine Service Route
                .route("execution-engine-service", r -> r
                        .path("/api/executions/**")
                        .filters(f -> f
                                .rewritePath("/api/executions/(?<segment>.*)", "/execution-engine/$1")
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver()))
                                .circuitBreaker(c -> c
                                        .setName("executionEngineCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/execution-engine")))
                        .uri("lb://execution-engine-service"))  // Using service discovery

                // LLM Processing Service Route
                .route("llm-processing-service", r -> r
                        .path("/api/llm/**")
                        .filters(f -> f
                                .rewritePath("/api/llm/(?<segment>.*)", "/llm-processor/$1")
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(ipKeyResolver()))
                                .circuitBreaker(c -> c
                                        .setName("llmProcessingCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/llm-processor")))
                        .uri("lb://llm-processing-service"))  // Using service discovery

                // Add a route for the Config Server (useful for diagnostics)
                .route("config-server", r -> r
                        .path("/config/**")
                        .filters(f -> f
                                .rewritePath("/config/(?<segment>.*)", "/$1")
                                .addRequestHeader("X-Gateway-Source", "api-gateway"))
                        .uri("lb://config-server"))

                // Add a route for Eureka Server dashboard access
                .route("eureka-server", r -> r
                        .path("/eureka/web")
                        .filters(f -> f
                                .rewritePath("/eureka/web", "/")
                                .addRequestHeader("X-Gateway-Source", "api-gateway"))
                        .uri("http://localhost:8761"))
                .route("eureka-server-static", r -> r
                        .path("/eureka/**")
                        .uri("http://localhost:8761"))

                .build();
    }

    /**
     * Rate limiter using Redis.
     * This can be replaced with a simpler rate limiter if Redis is not available.
     *
     * @return A configured RedisRateLimiter
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // Configure rate limiter with:
        // - 10 requests per second as the default replenish rate
        // - 20 requests as the default burst capacity
        return new RedisRateLimiter(10, 20);
    }

    /**
     * Key resolver for rate limiting based on the client's IP address.
     *
     * @return A KeyResolver that extracts the client IP address
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just(ip);
        };
    }
}