package binhnvh.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling fallback requests when a service is unavailable.
 * This is used in conjunction with circuit breakers to provide graceful degradation.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    /**
     * Generic fallback handler for any service.
     *
     * @param service The name of the service that failed
     * @return A response indicating the service is unavailable
     */
    @GetMapping(value = "/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String service) {
        logger.warn("Fallback triggered for service: {}", service);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Service temporarily unavailable");
        response.put("service", service);

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }

    /**
     * Specific fallback for the Tool Registry service.
     *
     * @return A response specific to the Tool Registry service
     */
    @GetMapping(value = "/tool-registry", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> toolRegistryFallback() {
        logger.warn("Fallback triggered for Tool Registry service");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Tool Registry service is temporarily unavailable");
        response.put("suggestion", "Please try again later or contact support if the issue persists");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }

    /**
     * Specific fallback for the Flow Registry service.
     *
     * @return A response specific to the Flow Registry service
     */
    @GetMapping(value = "/flow-registry", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> flowRegistryFallback() {
        logger.warn("Fallback triggered for Flow Registry service");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Flow Registry service is temporarily unavailable");
        response.put("suggestion", "Please try again later or contact support if the issue persists");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }

    /**
     * Specific fallback for the Execution Engine service.
     *
     * @return A response specific to the Execution Engine service
     */
    @GetMapping(value = "/execution-engine", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> executionEngineFallback() {
        logger.warn("Fallback triggered for Execution Engine service");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Execution Engine service is temporarily unavailable");
        response.put("suggestion", "Your request has been received but cannot be processed at the moment. Please check status later");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }

    /**
     * Specific fallback for the LLM Processor service.
     *
     * @return A response specific to the LLM Processor service
     */
    @GetMapping(value = "/llm-processor", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> llmProcessorFallback() {
        logger.warn("Fallback triggered for LLM Processor service");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "LLM Processing service is temporarily unavailable");
        response.put("suggestion", "Natural language processing is currently unavailable. Please try using direct API calls");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}