package com.kingname.embeddingstoremanager.health;

import com.kingname.embeddingstoremanager.EmbeddingCacheManagerConfig;
import com.kingname.embeddingstoremanager.EmbeddingCacheStore;
import com.kingname.embeddingstoremanager.EmbeddingGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class HealthCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    
    private final EmbeddingCacheManagerConfig config;
    private final EmbeddingCacheStore cacheStore;
    private final EmbeddingGenerator embeddingGenerator;
    private final CircuitBreaker circuitBreaker;
    
    public HealthCheckService(EmbeddingCacheManagerConfig config, 
                            EmbeddingCacheStore cacheStore, 
                            EmbeddingGenerator embeddingGenerator,
                            CircuitBreaker circuitBreaker) {
        this.config = config;
        this.cacheStore = cacheStore;
        this.embeddingGenerator = embeddingGenerator;
        this.circuitBreaker = circuitBreaker;
    }
    
    /**
     * Performs comprehensive health check
     */
    public HealthCheck performHealthCheck() {
        logger.debug("Starting health check");
        
        Map<String, HealthCheck.ComponentHealth> components = new HashMap<>();
        
        // Check cache store (Elasticsearch)
        HealthCheck.ComponentHealth cacheHealth = checkCacheStore();
        components.put("elasticsearch", cacheHealth);
        
        // Check embedding API
        HealthCheck.ComponentHealth apiHealth = checkEmbeddingApi();
        components.put("embedding-api", apiHealth);
        
        // Check circuit breaker status
        HealthCheck.ComponentHealth circuitBreakerHealth = checkCircuitBreaker();
        components.put("circuit-breaker", circuitBreakerHealth);
        
        // Determine overall status
        HealthCheck.HealthStatus overallStatus = determineOverallStatus(components);
        String message = generateOverallMessage(overallStatus, components);
        
        HealthCheck healthCheck = HealthCheck.builder()
                .status(overallStatus)
                .timestamp(LocalDateTime.now())
                .message(message)
                .components(components)
                .build();
        
        logger.info("Health check completed with status: {}", overallStatus);
        return healthCheck;
    }
    
    /**
     * Checks Elasticsearch cache store connectivity
     */
    private HealthCheck.ComponentHealth checkCacheStore() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Try to get a non-existent embedding to test connectivity
            cacheStore.getCachedEmbedding("__health_check__");
            long responseTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> details = new HashMap<>();
            details.put("hosts", config.getElasticSearchCacheHosts());
            details.put("port", config.getElasticSearchCachePort());
            details.put("alias", config.getElasticSearchCacheAliasName());
            
            return HealthCheck.ComponentHealth.builder()
                    .status(HealthCheck.HealthStatus.UP)
                    .message("Elasticsearch connection healthy")
                    .responseTimeMs(responseTime)
                    .details(details)
                    .build();
                    
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.warn("Elasticsearch health check failed: {}", e.getMessage());
            
            Map<String, Object> details = new HashMap<>();
            details.put("error", e.getMessage());
            details.put("hosts", config.getElasticSearchCacheHosts());
            
            return HealthCheck.ComponentHealth.builder()
                    .status(HealthCheck.HealthStatus.DOWN)
                    .message("Elasticsearch connection failed: " + e.getMessage())
                    .responseTimeMs(responseTime)
                    .details(details)
                    .build();
        }
    }
    
    /**
     * Checks embedding API connectivity (basic connectivity test)
     */
    private HealthCheck.ComponentHealth checkEmbeddingApi() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Use CompletableFuture with timeout for API check
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Simple connectivity test - try to generate a minimal embedding
                    embeddingGenerator.generateEmbedding("health check");
                    return "API accessible";
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            future.get(5, TimeUnit.SECONDS); // 5 second timeout
            long responseTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> details = new HashMap<>();
            details.put("url", config.getEmbeddingApiUrl());
            details.put("model", config.getModelName());
            details.put("timeout_ms", config.getSocketTimeoutMs());
            
            return HealthCheck.ComponentHealth.builder()
                    .status(HealthCheck.HealthStatus.UP)
                    .message("Embedding API accessible")
                    .responseTimeMs(responseTime)
                    .details(details)
                    .build();
                    
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.warn("Embedding API health check failed: {}", e.getMessage());
            
            Map<String, Object> details = new HashMap<>();
            details.put("error", e.getMessage());
            details.put("url", config.getEmbeddingApiUrl());
            
            return HealthCheck.ComponentHealth.builder()
                    .status(HealthCheck.HealthStatus.DOWN)
                    .message("Embedding API not accessible: " + e.getMessage())
                    .responseTimeMs(responseTime)
                    .details(details)
                    .build();
        }
    }
    
    /**
     * Checks circuit breaker status
     */
    private HealthCheck.ComponentHealth checkCircuitBreaker() {
        try {
            if (circuitBreaker == null) {
                return HealthCheck.ComponentHealth.builder()
                        .status(HealthCheck.HealthStatus.UNKNOWN)
                        .message("Circuit breaker disabled")
                        .responseTimeMs(0L)
                        .details(Map.of("enabled", false))
                        .build();
            }
            
            CircuitBreaker.State state = circuitBreaker.getState();
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            
            Map<String, Object> details = new HashMap<>();
            details.put("state", state.toString());
            details.put("failure_rate", metrics.getFailureRate());
            details.put("number_of_calls", metrics.getNumberOfBufferedCalls());
            details.put("number_of_failed_calls", metrics.getNumberOfFailedCalls());
            
            HealthCheck.HealthStatus status = determineCircuitBreakerHealth(state);
            String message = String.format("Circuit breaker state: %s (%.1f%% failure rate)", 
                                          state, metrics.getFailureRate());
            
            return HealthCheck.ComponentHealth.builder()
                    .status(status)
                    .message(message)
                    .responseTimeMs(0L)
                    .details(details)
                    .build();
                    
        } catch (Exception e) {
            logger.warn("Circuit breaker health check failed: {}", e.getMessage());
            
            return HealthCheck.ComponentHealth.builder()
                    .status(HealthCheck.HealthStatus.UNKNOWN)
                    .message("Circuit breaker status unknown: " + e.getMessage())
                    .responseTimeMs(0L)
                    .details(Map.of("error", e.getMessage()))
                    .build();
        }
    }
    
    private HealthCheck.HealthStatus determineCircuitBreakerHealth(CircuitBreaker.State state) {
        switch (state) {
            case CLOSED:
                return HealthCheck.HealthStatus.UP;
            case HALF_OPEN:
                return HealthCheck.HealthStatus.DEGRADED;
            case OPEN:
                return HealthCheck.HealthStatus.DOWN;
            default:
                return HealthCheck.HealthStatus.UNKNOWN;
        }
    }
    
    private HealthCheck.HealthStatus determineOverallStatus(Map<String, HealthCheck.ComponentHealth> components) {
        boolean hasDown = components.values().stream()
                .anyMatch(c -> c.getStatus() == HealthCheck.HealthStatus.DOWN);
        
        if (hasDown) {
            return HealthCheck.HealthStatus.DOWN;
        }
        
        boolean hasDegraded = components.values().stream()
                .anyMatch(c -> c.getStatus() == HealthCheck.HealthStatus.DEGRADED);
        
        if (hasDegraded) {
            return HealthCheck.HealthStatus.DEGRADED;
        }
        
        boolean allUp = components.values().stream()
                .allMatch(c -> c.getStatus() == HealthCheck.HealthStatus.UP);
        
        return allUp ? HealthCheck.HealthStatus.UP : HealthCheck.HealthStatus.UNKNOWN;
    }
    
    private String generateOverallMessage(HealthCheck.HealthStatus status, 
                                        Map<String, HealthCheck.ComponentHealth> components) {
        long upCount = components.values().stream()
                .mapToLong(c -> c.getStatus() == HealthCheck.HealthStatus.UP ? 1 : 0)
                .sum();
        
        return String.format("Overall status: %s (%d/%d components healthy)", 
                           status, upCount, components.size());
    }
}