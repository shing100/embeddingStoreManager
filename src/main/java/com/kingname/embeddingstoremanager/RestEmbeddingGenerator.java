package com.kingname.embeddingstoremanager;

import com.google.gson.Gson;
import com.kingname.embeddingstoremanager.exception.RestEmbeddingGeneratorException;
import com.kingname.embeddingstoremanager.vo.EmbeddingResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class RestEmbeddingGenerator implements EmbeddingGenerator {

    private static final Logger logger = LoggerFactory.getLogger(RestEmbeddingGenerator.class);
    
    private final Gson gson = new Gson();
    private final EmbeddingCacheManagerConfig ecmConfig;
    private final CloseableHttpClient httpClient;

    public RestEmbeddingGenerator(EmbeddingCacheManagerConfig ecmConfig) {
        this.ecmConfig = ecmConfig;
        this.httpClient = createHttpClient();
        logger.info("RestEmbeddingGenerator initialized with connection pooling");
    }
    
    /**
     * Creates HTTP client with connection pooling and proper timeouts
     */
    private CloseableHttpClient createHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(ecmConfig.getMaxConnections());
        connectionManager.setDefaultMaxPerRoute(ecmConfig.getMaxConnectionsPerRoute());
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(5000) // Timeout for getting connection from pool
                .setConnectTimeout(ecmConfig.getConnectionTimeoutMs())
                .setSocketTimeout(ecmConfig.getSocketTimeoutMs())
                .build();
        
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @Override
    public List<Double> generateEmbedding(String text) throws RestEmbeddingGeneratorException {
        logger.debug("Generating embedding for text with length: {}", text != null ? text.length() : 0);
        
        // Input validation for SSRF prevention
        validateApiUrl(this.ecmConfig.getEmbeddingApiUrl());
        
        try {
            HttpPost httpPost = new HttpPost(this.ecmConfig.getEmbeddingApiUrl());
            Map<String, String> body = new HashMap<>();
            body.put("input", text);
            body.put("model", this.ecmConfig.getModelName());
            StringEntity entity = new StringEntity(gson.toJson(body), "UTF-8");
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
            
            // Add authentication header if API key is provided
            if (ecmConfig.getApiKey() != null && !ecmConfig.getApiKey().trim().isEmpty()) {
                String headerName = ecmConfig.getApiKeyHeader() != null ? 
                    ecmConfig.getApiKeyHeader() : "Authorization";
                
                String headerValue = headerName.equalsIgnoreCase("Authorization") ? 
                    "Bearer " + ecmConfig.getApiKey() : ecmConfig.getApiKey();
                
                httpPost.setHeader(headerName, headerValue);
                logger.debug("Added authentication header: {}", headerName);
            }
            
            logger.debug("Sending embedding request to API: {}", this.ecmConfig.getEmbeddingApiUrl());
            
            // Use connection pooled HTTP client
            try (CloseableHttpResponse response = httpClient.execute(httpPost);
                 Scanner sc = new Scanner(response.getEntity().getContent())) {
                
                int statusCode = response.getStatusLine().getStatusCode();
                logger.debug("Received HTTP response with status code: {}", statusCode);
                
                if (statusCode != 200) {
                    logger.error("API returned error status: {}", statusCode);
                    throw new RestEmbeddingGeneratorException("API returned error status: " + statusCode, null);
                }
                
                StringBuilder responseBody = new StringBuilder();
                while(sc.hasNext()) {
                    responseBody.append(sc.nextLine());
                }
                
                EmbeddingResponse embeddingResponse = gson.fromJson(responseBody.toString(), EmbeddingResponse.class);
                
                // Validate response structure
                if (embeddingResponse == null || embeddingResponse.getData() == null || 
                    embeddingResponse.getData().isEmpty()) {
                    logger.error("Invalid response structure from embedding API");
                    throw new RestEmbeddingGeneratorException("Invalid response structure from embedding API", null);
                }
                
                List<Double> embedding = embeddingResponse.getData().get(0).getEmbedding();
                logger.debug("Successfully generated embedding with dimension: {}", embedding != null ? embedding.size() : 0);
                
                return embedding;
            }
        } catch (RestEmbeddingGeneratorException e) {
            logger.error("Embedding generation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during embedding generation: {}", e.getMessage(), e);
            throw new RestEmbeddingGeneratorException(e.getMessage(), e.getCause());
        }
    }
    
    /**
     * Validates API URL to prevent SSRF attacks
     */
    private void validateApiUrl(String url) throws RestEmbeddingGeneratorException {
        if (url == null || url.trim().isEmpty()) {
            logger.error("API URL validation failed: URL is null or empty");
            throw new RestEmbeddingGeneratorException("API URL cannot be null or empty", null);
        }
        
        // Only allow HTTPS URLs for security
        if (!url.toLowerCase().startsWith("https://")) {
            logger.error("API URL validation failed: Non-HTTPS URL attempted: {}", url);
            throw new RestEmbeddingGeneratorException("Only HTTPS URLs are allowed for security", null);
        }
        
        // Block localhost and private IP ranges
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("localhost") || 
            lowerUrl.contains("127.0.0.1") ||
            lowerUrl.contains("10.") ||
            lowerUrl.contains("192.168.") ||
            lowerUrl.matches(".*172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
            logger.error("API URL validation failed: Private network URL attempted: {}", url);
            throw new RestEmbeddingGeneratorException("Private network URLs are not allowed", null);
        }
        
        logger.debug("API URL validation passed: {}", url);
    }
}
