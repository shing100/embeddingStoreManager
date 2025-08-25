package com.kingname.embeddingstoremanager;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Builder
@Getter
@ToString
public class EmbeddingCacheManagerConfig {
    private final List<String> elasticSearchCacheHosts;
    private final Integer elasticSearchCachePort;
    private final String elasticSearchCacheAliasName;
    @Builder.Default
    private final Integer retentionMonth = 3;
    private final String modelName;
    private final String embeddingApiUrl;
    @Builder.Default
    private final Integer maxLength = 3_000;
    
    // Authentication support
    private final String apiKey;           // API Key for Bearer token authentication
    private final String apiKeyHeader;     // Custom header name for API key (default: "Authorization")
    
    @Builder.Default
    private final Integer connectionTimeoutMs = 10_000;  // 10 seconds
    @Builder.Default  
    private final Integer socketTimeoutMs = 30_000;      // 30 seconds
    @Builder.Default
    private final Integer maxConnections = 20;           // Max total connections
    @Builder.Default
    private final Integer maxConnectionsPerRoute = 10;   // Max connections per route
    
    // Circuit Breaker configuration
    @Builder.Default
    private final Boolean enableCircuitBreaker = true;   // Enable circuit breaker
    @Builder.Default
    private final Integer circuitBreakerFailureThreshold = 5; // Number of failures to open circuit
    @Builder.Default
    private final Long circuitBreakerWaitDurationMs = 60_000L; // Wait time in open state (1 minute)
    @Builder.Default
    private final Integer circuitBreakerMinimumNumberOfCalls = 10; // Minimum calls before evaluation
    @Builder.Default
    private final Float circuitBreakerFailureRateThreshold = 50.0f; // Failure rate % to open circuit
    
    // Retry configuration
    @Builder.Default
    private final Boolean enableRetry = true;            // Enable retry mechanism
    @Builder.Default
    private final Integer maxRetryAttempts = 3;          // Maximum retry attempts
    @Builder.Default
    private final Long retryWaitDurationMs = 1_000L;     // Wait time between retries (1 second)
    
    // Metrics configuration
    @Builder.Default
    private final Boolean enableMetrics = true;          // Enable metrics collection
}
