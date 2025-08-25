package com.kingname.embeddingstoremanager.health;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@ToString
public class HealthCheck {
    private final HealthStatus status;
    private final LocalDateTime timestamp;
    private final String message;
    private final Map<String, ComponentHealth> components;
    
    public enum HealthStatus {
        UP, DOWN, DEGRADED, UNKNOWN
    }
    
    @Getter
    @Builder
    @ToString
    public static class ComponentHealth {
        private final HealthStatus status;
        private final String message;
        private final Long responseTimeMs;
        private final Map<String, Object> details;
    }
}