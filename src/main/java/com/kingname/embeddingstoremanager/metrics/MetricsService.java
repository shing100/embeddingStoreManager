package com.kingname.embeddingstoremanager.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Metrics service for tracking embedding operations performance and usage
 */
public class MetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    private final MeterRegistry meterRegistry;
    private final boolean enabled;
    
    // Counters
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter embeddingRequests;
    private final Counter embeddingSuccess;
    private final Counter embeddingFailures;
    private final Counter healthCheckRequests;
    
    // Timers
    private final Timer cacheRetrievalTime;
    private final Timer embeddingGenerationTime;
    private final Timer totalRequestTime;
    private final Timer healthCheckTime;
    
    public MetricsService(boolean enabled) {
        this.enabled = enabled;
        
        if (enabled) {
            this.meterRegistry = new SimpleMeterRegistry();
            
            // Initialize counters
            this.cacheHits = Counter.builder("embedding.cache.hits")
                    .description("Number of successful cache retrievals")
                    .register(meterRegistry);
            
            this.cacheMisses = Counter.builder("embedding.cache.misses")
                    .description("Number of cache misses requiring generation")
                    .register(meterRegistry);
            
            this.embeddingRequests = Counter.builder("embedding.requests.total")
                    .description("Total number of embedding requests")
                    .register(meterRegistry);
            
            this.embeddingSuccess = Counter.builder("embedding.generation.success")
                    .description("Number of successful embedding generations")
                    .register(meterRegistry);
            
            this.embeddingFailures = Counter.builder("embedding.generation.failures")
                    .description("Number of failed embedding generations")
                    .register(meterRegistry);
            
            this.healthCheckRequests = Counter.builder("health.check.requests")
                    .description("Number of health check requests")
                    .register(meterRegistry);
            
            // Initialize timers
            this.cacheRetrievalTime = Timer.builder("embedding.cache.retrieval.time")
                    .description("Time taken to retrieve embeddings from cache")
                    .register(meterRegistry);
            
            this.embeddingGenerationTime = Timer.builder("embedding.generation.time")
                    .description("Time taken to generate embeddings")
                    .register(meterRegistry);
            
            this.totalRequestTime = Timer.builder("embedding.request.total.time")
                    .description("Total time for embedding requests (cache + generation)")
                    .register(meterRegistry);
            
            this.healthCheckTime = Timer.builder("health.check.time")
                    .description("Time taken for health checks")
                    .register(meterRegistry);
            
            logger.info("MetricsService initialized with metrics collection enabled");
        } else {
            // Initialize with null values when disabled
            this.meterRegistry = null;
            this.cacheHits = null;
            this.cacheMisses = null;
            this.embeddingRequests = null;
            this.embeddingSuccess = null;
            this.embeddingFailures = null;
            this.healthCheckRequests = null;
            this.cacheRetrievalTime = null;
            this.embeddingGenerationTime = null;
            this.totalRequestTime = null;
            this.healthCheckTime = null;
            
            logger.info("MetricsService initialized with metrics collection disabled");
        }
    }
    
    /**
     * Record a cache hit event
     */
    public void recordCacheHit() {
        if (enabled && cacheHits != null) {
            cacheHits.increment();
        }
    }
    
    /**
     * Record a cache miss event
     */
    public void recordCacheMiss() {
        if (enabled && cacheMisses != null) {
            cacheMisses.increment();
        }
    }
    
    /**
     * Record an embedding request
     */
    public void recordEmbeddingRequest() {
        if (enabled && embeddingRequests != null) {
            embeddingRequests.increment();
        }
    }
    
    /**
     * Record successful embedding generation
     */
    public void recordEmbeddingSuccess() {
        if (enabled && embeddingSuccess != null) {
            embeddingSuccess.increment();
        }
    }
    
    /**
     * Record failed embedding generation
     */
    public void recordEmbeddingFailure() {
        if (enabled && embeddingFailures != null) {
            embeddingFailures.increment();
        }
    }
    
    /**
     * Record a health check request
     */
    public void recordHealthCheckRequest() {
        if (enabled && healthCheckRequests != null) {
            healthCheckRequests.increment();
        }
    }
    
    /**
     * Time cache retrieval operations
     */
    public Timer.Sample startCacheRetrievalTimer() {
        if (enabled && cacheRetrievalTime != null) {
            return Timer.start(meterRegistry);
        }
        return null;
    }
    
    /**
     * Stop cache retrieval timer
     */
    public void stopCacheRetrievalTimer(Timer.Sample sample) {
        if (enabled && sample != null && cacheRetrievalTime != null) {
            sample.stop(cacheRetrievalTime);
        }
    }
    
    /**
     * Time embedding generation operations
     */
    public Timer.Sample startEmbeddingGenerationTimer() {
        if (enabled && embeddingGenerationTime != null) {
            return Timer.start(meterRegistry);
        }
        return null;
    }
    
    /**
     * Stop embedding generation timer
     */
    public void stopEmbeddingGenerationTimer(Timer.Sample sample) {
        if (enabled && sample != null && embeddingGenerationTime != null) {
            sample.stop(embeddingGenerationTime);
        }
    }
    
    /**
     * Time total request operations
     */
    public Timer.Sample startTotalRequestTimer() {
        if (enabled && totalRequestTime != null) {
            return Timer.start(meterRegistry);
        }
        return null;
    }
    
    /**
     * Stop total request timer
     */
    public void stopTotalRequestTimer(Timer.Sample sample) {
        if (enabled && sample != null && totalRequestTime != null) {
            sample.stop(totalRequestTime);
        }
    }
    
    /**
     * Time health check operations
     */
    public Timer.Sample startHealthCheckTimer() {
        if (enabled && healthCheckTime != null) {
            return Timer.start(meterRegistry);
        }
        return null;
    }
    
    /**
     * Stop health check timer
     */
    public void stopHealthCheckTimer(Timer.Sample sample) {
        if (enabled && sample != null && healthCheckTime != null) {
            sample.stop(healthCheckTime);
        }
    }
    
    /**
     * Get the meter registry for custom metrics
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    /**
     * Check if metrics collection is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get current metrics summary
     */
    public MetricsSummary getMetricsSummary() {
        if (!enabled || meterRegistry == null) {
            return MetricsSummary.disabled();
        }
        
        return MetricsSummary.builder()
                .cacheHitRate(calculateCacheHitRate())
                .totalRequests(embeddingRequests.count())
                .successfulGenerations(embeddingSuccess.count())
                .failedGenerations(embeddingFailures.count())
                .averageCacheRetrievalTime(getAverageTime(cacheRetrievalTime))
                .averageGenerationTime(getAverageTime(embeddingGenerationTime))
                .averageTotalRequestTime(getAverageTime(totalRequestTime))
                .healthCheckRequests(healthCheckRequests.count())
                .averageHealthCheckTime(getAverageTime(healthCheckTime))
                .enabled(true)
                .build();
    }
    
    private double calculateCacheHitRate() {
        double hits = cacheHits.count();
        double total = hits + cacheMisses.count();
        return total > 0 ? (hits / total) * 100.0 : 0.0;
    }
    
    private double getAverageTime(Timer timer) {
        return timer != null ? timer.mean(TimeUnit.MILLISECONDS) : 0.0;
    }
}