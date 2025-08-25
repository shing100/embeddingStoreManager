package com.kingname.embeddingstoremanager;

import com.kingname.embeddingstoremanager.exception.ElasticSearchClientException;
import com.kingname.embeddingstoremanager.exception.EmbeddingCacheManagerException;
import com.kingname.embeddingstoremanager.exception.EmbeddingCacheStoreException;
import com.kingname.embeddingstoremanager.exception.EmbeddingGeneratorException;
import com.kingname.embeddingstoremanager.async.AsyncEmbeddingService;
import com.kingname.embeddingstoremanager.health.HealthCheck;
import com.kingname.embeddingstoremanager.health.HealthCheckService;
import com.kingname.embeddingstoremanager.metrics.MetricsService;
import com.kingname.embeddingstoremanager.metrics.MetricsSummary;
import com.kingname.embeddingstoremanager.vo.CachedEmbeddingDocument;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EmbeddingCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingCacheManager.class);
    
    private final EmbeddingCacheStore embeddingCacheStore;
    private final EmbeddingGenerator openAIEmbeddingGenerator;
    private final EmbeddingCacheManagerConfig embeddingCacheManagerConfig;
    private final HealthCheckService healthCheckService;
    private final MetricsService metricsService;

    public EmbeddingCacheManager(EmbeddingCacheManagerConfig ecmConfig) throws EmbeddingCacheManagerException {
        this(ecmConfig, new ESEmbeddingCacheStore(ecmConfig), new RestEmbeddingGenerator(ecmConfig));
    }

    public EmbeddingCacheManager(EmbeddingCacheManagerConfig ecmConfig, EmbeddingCacheStore esEmbeddingCacheStore) {
        this(ecmConfig, esEmbeddingCacheStore, new RestEmbeddingGenerator(ecmConfig));
    }

    public EmbeddingCacheManager(EmbeddingCacheManagerConfig ecmConfig, EmbeddingGenerator openAIEmbeddingGenerator) throws ElasticSearchClientException {
        this(ecmConfig, new ESEmbeddingCacheStore(ecmConfig), openAIEmbeddingGenerator);
    }

    public EmbeddingCacheManager(EmbeddingCacheManagerConfig embeddingCacheManagerConfig, EmbeddingCacheStore esEmbeddingCacheStore, EmbeddingGenerator openAIEmbeddingGenerator) {
        this.embeddingCacheStore = esEmbeddingCacheStore;
        this.openAIEmbeddingGenerator = openAIEmbeddingGenerator;
        this.embeddingCacheManagerConfig = embeddingCacheManagerConfig;
        
        // Initialize health check service
        CircuitBreaker circuitBreaker = null;
        if (openAIEmbeddingGenerator instanceof RestEmbeddingGenerator) {
            circuitBreaker = ((RestEmbeddingGenerator) openAIEmbeddingGenerator).getCircuitBreaker();
        }
        
        this.healthCheckService = new HealthCheckService(
            embeddingCacheManagerConfig, 
            esEmbeddingCacheStore, 
            openAIEmbeddingGenerator, 
            circuitBreaker
        );
        
        // Initialize metrics service
        this.metricsService = new MetricsService(embeddingCacheManagerConfig.getEnableMetrics());
        
        logger.info("EmbeddingCacheManager initialized with health checking and metrics collection enabled");
    }

    public List<Double> getEmbedding(String text) throws EmbeddingCacheStoreException, EmbeddingGeneratorException {
        metricsService.recordEmbeddingRequest();
        Timer.Sample totalTimer = metricsService.startTotalRequestTimer();
        
        try {
            List<Double> embedding = getEmbeddingFromCache(text);
            if(Objects.isNull(embedding)) {
                metricsService.recordCacheMiss();
                embedding = generateEmbedding(text);
                storeEmbedding(text, embedding);
                metricsService.recordEmbeddingSuccess();
            } else {
                metricsService.recordCacheHit();
            }
            return embedding;
        } catch (Exception e) {
            metricsService.recordEmbeddingFailure();
            throw e;
        } finally {
            metricsService.stopTotalRequestTimer(totalTimer);
        }
    }

    public List<Double> getEmbeddingFromCache(String text) throws EmbeddingCacheStoreException {
        Timer.Sample cacheTimer = metricsService.startCacheRetrievalTimer();
        try {
            return embeddingCacheStore.getCachedEmbedding(normalize(text));
        } finally {
            metricsService.stopCacheRetrievalTimer(cacheTimer);
        }
    }

    public List<Double> generateEmbedding(String text) throws EmbeddingGeneratorException {
        Timer.Sample generationTimer = metricsService.startEmbeddingGenerationTimer();
        try {
            return openAIEmbeddingGenerator.generateEmbedding(normalize(text));
        } finally {
            metricsService.stopEmbeddingGenerationTimer(generationTimer);
        }
    }

    public void storeEmbedding(String text, List<Double> embedding) throws EmbeddingCacheStoreException {
        this.embeddingCacheStore.storeEmbedding(normalize(text), embedding);
    }

    public void storeEmbedding(CachedEmbeddingDocument document) throws EmbeddingCacheStoreException {
        this.embeddingCacheStore.storeEmbedding(document);
    }

    public void storeEmbeddings(List<CachedEmbeddingDocument> documents) throws EmbeddingCacheStoreException {
        this.embeddingCacheStore.storeEmbeddings(documents);
    }

    public String normalize(String text) {
        return text.substring(0, Math.min(text.length(), embeddingCacheManagerConfig.getMaxLength()))
                .trim()
                .toLowerCase(Locale.ROOT);
    }
    
    /**
     * Performs comprehensive health check of all system components
     * 
     * @return HealthCheck object containing overall status and component details
     */
    public HealthCheck performHealthCheck() {
        logger.debug("Performing health check for embedding cache manager");
        metricsService.recordHealthCheckRequest();
        Timer.Sample healthCheckTimer = metricsService.startHealthCheckTimer();
        
        try {
            return healthCheckService.performHealthCheck();
        } finally {
            metricsService.stopHealthCheckTimer(healthCheckTimer);
        }
    }
    
    /**
     * Get current metrics summary
     * 
     * @return MetricsSummary containing performance and usage metrics
     */
    public MetricsSummary getMetrics() {
        logger.debug("Retrieving metrics summary");
        return metricsService.getMetricsSummary();
    }
    
    /**
     * Get the metrics service for custom metrics registration
     * 
     * @return MetricsService instance
     */
    public MetricsService getMetricsService() {
        return this.metricsService;
    }
    
    /**
     * Create an asynchronous embedding service with default thread pool
     * 
     * @return AsyncEmbeddingService instance for non-blocking operations
     */
    public AsyncEmbeddingService createAsyncService() {
        logger.debug("Creating async embedding service with default configuration");
        return new AsyncEmbeddingService(this);
    }
    
    /**
     * Create an asynchronous embedding service with custom executors
     * 
     * @param executorService Custom executor service for async operations
     * @param scheduledExecutorService Custom scheduled executor service for periodic tasks
     * @return AsyncEmbeddingService instance with custom executors
     */
    public AsyncEmbeddingService createAsyncService(java.util.concurrent.ExecutorService executorService,
                                                   java.util.concurrent.ScheduledExecutorService scheduledExecutorService) {
        logger.debug("Creating async embedding service with custom executors");
        return new AsyncEmbeddingService(this, executorService, scheduledExecutorService);
    }

    public EmbeddingCacheStore getEmbeddingCacheStore() {
        return this.embeddingCacheStore;
    }

    public EmbeddingGenerator getOpenAIEmbeddingGenerator() {
        return this.openAIEmbeddingGenerator;
    }
}
