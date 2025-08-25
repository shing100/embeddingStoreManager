package com.kingname.embeddingstoremanager.metrics;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Summary of metrics collected by the MetricsService
 */
@Getter
@Builder
@ToString
public class MetricsSummary {
    
    private final boolean enabled;
    private final double cacheHitRate; // Percentage
    private final double totalRequests;
    private final double successfulGenerations;
    private final double failedGenerations;
    private final double averageCacheRetrievalTime; // Milliseconds
    private final double averageGenerationTime; // Milliseconds
    private final double averageTotalRequestTime; // Milliseconds
    private final double healthCheckRequests;
    private final double averageHealthCheckTime; // Milliseconds
    
    /**
     * Create a summary for disabled metrics
     */
    public static MetricsSummary disabled() {
        return MetricsSummary.builder()
                .enabled(false)
                .cacheHitRate(0.0)
                .totalRequests(0.0)
                .successfulGenerations(0.0)
                .failedGenerations(0.0)
                .averageCacheRetrievalTime(0.0)
                .averageGenerationTime(0.0)
                .averageTotalRequestTime(0.0)
                .healthCheckRequests(0.0)
                .averageHealthCheckTime(0.0)
                .build();
    }
    
    /**
     * Get success rate as percentage
     */
    public double getSuccessRate() {
        if (!enabled || totalRequests == 0) {
            return 0.0;
        }
        return (successfulGenerations / totalRequests) * 100.0;
    }
    
    /**
     * Get failure rate as percentage
     */
    public double getFailureRate() {
        if (!enabled || totalRequests == 0) {
            return 0.0;
        }
        return (failedGenerations / totalRequests) * 100.0;
    }
    
    /**
     * Check if metrics show healthy performance
     */
    public boolean isHealthy() {
        if (!enabled) {
            return true; // Assume healthy if metrics disabled
        }
        
        // Consider healthy if:
        // - Success rate > 95%
        // - Average response time < 5000ms
        // - Cache hit rate > 20% (if there are requests)
        return getSuccessRate() > 95.0 
            && averageTotalRequestTime < 5000.0
            && (totalRequests == 0 || cacheHitRate > 20.0);
    }
    
    /**
     * Get performance grade based on metrics
     */
    public PerformanceGrade getPerformanceGrade() {
        if (!enabled) {
            return PerformanceGrade.UNKNOWN;
        }
        
        double successRate = getSuccessRate();
        
        if (successRate >= 99.0 && averageTotalRequestTime < 1000.0 && cacheHitRate > 80.0) {
            return PerformanceGrade.EXCELLENT;
        } else if (successRate >= 95.0 && averageTotalRequestTime < 3000.0 && cacheHitRate > 50.0) {
            return PerformanceGrade.GOOD;
        } else if (successRate >= 90.0 && averageTotalRequestTime < 5000.0) {
            return PerformanceGrade.FAIR;
        } else {
            return PerformanceGrade.POOR;
        }
    }
    
    public enum PerformanceGrade {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }
}