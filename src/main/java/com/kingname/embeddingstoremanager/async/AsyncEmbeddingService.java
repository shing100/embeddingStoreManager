package com.kingname.embeddingstoremanager.async;

import com.kingname.embeddingstoremanager.EmbeddingCacheManager;
import com.kingname.embeddingstoremanager.EmbeddingCacheManagerConfig;
import com.kingname.embeddingstoremanager.exception.EmbeddingCacheStoreException;
import com.kingname.embeddingstoremanager.exception.EmbeddingGeneratorException;
import com.kingname.embeddingstoremanager.health.HealthCheck;
import com.kingname.embeddingstoremanager.metrics.MetricsSummary;
import com.kingname.embeddingstoremanager.vo.CachedEmbeddingDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Asynchronous embedding service providing non-blocking operations
 */
public class AsyncEmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncEmbeddingService.class);
    
    private final EmbeddingCacheManager embeddingCacheManager;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final EmbeddingCacheManagerConfig config;
    private final boolean shutdownExecutorOnClose;
    
    /**
     * Constructor with default thread pool configuration
     */
    public AsyncEmbeddingService(EmbeddingCacheManager embeddingCacheManager) {
        this.embeddingCacheManager = embeddingCacheManager;
        this.config = null; // Will use defaults
        
        // Create default thread pools
        int threadPoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.executorService = Executors.newFixedThreadPool(
            threadPoolSize,
            r -> {
                Thread t = new Thread(r, "async-embedding-" + r.hashCode());
                t.setDaemon(true);
                return t;
            }
        );
        this.scheduledExecutorService = Executors.newScheduledThreadPool(2);
        this.shutdownExecutorOnClose = true;
        
        logger.info("AsyncEmbeddingService initialized with default thread pool (size: {})", threadPoolSize);
    }
    
    /**
     * Constructor with custom thread pool
     */
    public AsyncEmbeddingService(EmbeddingCacheManager embeddingCacheManager, 
                                ExecutorService executorService,
                                ScheduledExecutorService scheduledExecutorService) {
        this.embeddingCacheManager = embeddingCacheManager;
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.config = null;
        this.shutdownExecutorOnClose = false; // Don't shutdown externally provided executors
        
        logger.info("AsyncEmbeddingService initialized with custom thread pool");
    }
    
    /**
     * Get embedding asynchronously
     * 
     * @param text Input text for embedding generation
     * @return CompletableFuture containing the embedding vector
     */
    public CompletableFuture<List<Double>> getEmbeddingAsync(String text) {
        logger.debug("Starting async embedding request for text length: {}", text != null ? text.length() : 0);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return embeddingCacheManager.getEmbedding(text);
            } catch (EmbeddingCacheStoreException | EmbeddingGeneratorException e) {
                logger.error("Async embedding generation failed: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Get embedding from cache only (async)
     * 
     * @param text Input text to search in cache
     * @return CompletableFuture containing cached embedding or null if not found
     */
    public CompletableFuture<List<Double>> getEmbeddingFromCacheAsync(String text) {
        logger.debug("Starting async cache lookup for text length: {}", text != null ? text.length() : 0);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return embeddingCacheManager.getEmbeddingFromCache(text);
            } catch (EmbeddingCacheStoreException e) {
                logger.error("Async cache lookup failed: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Generate embedding without checking cache (async)
     * 
     * @param text Input text for embedding generation
     * @return CompletableFuture containing the generated embedding
     */
    public CompletableFuture<List<Double>> generateEmbeddingAsync(String text) {
        logger.debug("Starting async embedding generation for text length: {}", text != null ? text.length() : 0);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return embeddingCacheManager.generateEmbedding(text);
            } catch (EmbeddingGeneratorException e) {
                logger.error("Async embedding generation failed: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Store embedding asynchronously
     * 
     * @param text Input text
     * @param embedding Generated embedding
     * @return CompletableFuture that completes when storage is finished
     */
    public CompletableFuture<Void> storeEmbeddingAsync(String text, List<Double> embedding) {
        logger.debug("Starting async embedding storage for text length: {}", text != null ? text.length() : 0);
        
        return CompletableFuture.runAsync(() -> {
            try {
                embeddingCacheManager.storeEmbedding(text, embedding);
            } catch (EmbeddingCacheStoreException e) {
                logger.error("Async embedding storage failed: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Store embeddings in bulk asynchronously
     * 
     * @param documents List of embedding documents to store
     * @return CompletableFuture that completes when bulk storage is finished
     */
    public CompletableFuture<Void> storeEmbeddingsAsync(List<CachedEmbeddingDocument> documents) {
        logger.debug("Starting async bulk embedding storage for {} documents", documents != null ? documents.size() : 0);
        
        return CompletableFuture.runAsync(() -> {
            try {
                embeddingCacheManager.storeEmbeddings(documents);
            } catch (EmbeddingCacheStoreException e) {
                logger.error("Async bulk embedding storage failed: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Perform health check asynchronously
     * 
     * @return CompletableFuture containing health check results
     */
    public CompletableFuture<HealthCheck> performHealthCheckAsync() {
        logger.debug("Starting async health check");
        
        return CompletableFuture.supplyAsync(() -> {
            return embeddingCacheManager.performHealthCheck();
        }, executorService);
    }
    
    /**
     * Get metrics asynchronously
     * 
     * @return CompletableFuture containing metrics summary
     */
    public CompletableFuture<MetricsSummary> getMetricsAsync() {
        logger.debug("Starting async metrics retrieval");
        
        return CompletableFuture.supplyAsync(() -> {
            return embeddingCacheManager.getMetrics();
        }, executorService);
    }
    
    /**
     * Process multiple texts in parallel
     * 
     * @param texts List of texts to process
     * @return CompletableFuture containing list of embeddings in the same order
     */
    public CompletableFuture<List<List<Double>>> getEmbeddingsBatchAsync(List<String> texts) {
        logger.debug("Starting async batch embedding processing for {} texts", texts != null ? texts.size() : 0);
        
        if (texts == null || texts.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        List<CompletableFuture<List<Double>>> futures = texts.stream()
                .map(this::getEmbeddingAsync)
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    /**
     * Schedule periodic health checks
     * 
     * @param intervalMinutes Interval between health checks in minutes
     * @param callback Callback to handle health check results
     */
    public void schedulePeriodicHealthCheck(int intervalMinutes, HealthCheckCallback callback) {
        logger.info("Scheduling periodic health checks every {} minutes", intervalMinutes);
        
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                performHealthCheckAsync()
                        .thenAccept(callback::onHealthCheck)
                        .exceptionally(throwable -> {
                            callback.onHealthCheckError(throwable);
                            return null;
                        });
            } catch (Exception e) {
                logger.error("Error in scheduled health check: {}", e.getMessage(), e);
                callback.onHealthCheckError(e);
            }
        }, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
    }
    
    /**
     * Shutdown the async service and clean up resources
     */
    public void shutdown() {
        logger.info("Shutting down AsyncEmbeddingService");
        
        if (shutdownExecutorOnClose) {
            executorService.shutdown();
            scheduledExecutorService.shutdown();
            
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate within 30 seconds, forcing shutdown");
                    executorService.shutdownNow();
                }
                if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Scheduled executor did not terminate within 5 seconds, forcing shutdown");
                    scheduledExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
                scheduledExecutorService.shutdownNow();
            }
        }
        
        logger.info("AsyncEmbeddingService shutdown completed");
    }
    
    /**
     * Get the underlying embedding cache manager
     */
    public EmbeddingCacheManager getEmbeddingCacheManager() {
        return embeddingCacheManager;
    }
    
    /**
     * Check if the service is still running
     */
    public boolean isRunning() {
        return !executorService.isShutdown();
    }
    
    @FunctionalInterface
    public interface HealthCheckCallback {
        void onHealthCheck(HealthCheck healthCheck);
        
        default void onHealthCheckError(Throwable error) {
            // Default implementation - can be overridden
        }
    }
}