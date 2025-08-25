package com.kingname.embeddingstoremanager;

import org.junit.Test;
import static org.assertj.core.api.Assertions.*;

public class EmbeddingStoreManagerApplicationTests {

    @Test
    public void testConfigurationBuilder() {
        // Test configuration builder
        EmbeddingCacheManagerConfig config = EmbeddingCacheManagerConfig.builder()
                .elasticSearchCacheHosts(java.util.Arrays.asList("localhost"))
                .elasticSearchCachePort(9200)
                .elasticSearchCacheAliasName("test-embeddings")
                .modelName("test-model")
                .embeddingApiUrl("http://test-api.com")
                .build();
        
        assertThat(config).isNotNull();
        assertThat(config.getElasticSearchCacheHosts()).containsExactly("localhost");
        assertThat(config.getElasticSearchCachePort()).isEqualTo(9200);
        assertThat(config.getRetentionMonth()).isEqualTo(3); // Default value
        assertThat(config.getMaxLength()).isEqualTo(3000); // Default value
    }

    @Test
    public void testHashGenerator() throws Exception {
        HashGenerator hashGenerator = new HashGenerator();
        String hash1 = hashGenerator.getHash("test string");
        String hash2 = hashGenerator.getHash("test string");
        String hash3 = hashGenerator.getHash("different string");
        
        assertThat(hash1).isNotNull();
        assertThat(hash1).isEqualTo(hash2); // Same input should produce same hash
        assertThat(hash1).isNotEqualTo(hash3); // Different input should produce different hash
        assertThat(hash1).hasSize(64); // SHA-256 produces 64 character hex string
    }

    @Test
    public void testTextNormalization() {
        EmbeddingCacheManagerConfig config = EmbeddingCacheManagerConfig.builder()
                .elasticSearchCacheHosts(java.util.Arrays.asList("localhost"))
                .elasticSearchCachePort(9200)
                .elasticSearchCacheAliasName("test")
                .modelName("test")
                .embeddingApiUrl("http://test.com")
                .maxLength(10)
                .build();
        
        EmbeddingCacheManager manager = new EmbeddingCacheManager(config, 
            new MockEmbeddingCacheStore(), new MockEmbeddingGenerator());
        
        // Test normalization
        assertThat(manager.normalize("UPPER CASE")).isEqualTo("upper case");
        assertThat(manager.normalize("  spaces  ")).isEqualTo("spaces");
        assertThat(manager.normalize("very long string that exceeds max")).isEqualTo("very long");
    }
    
    // Mock implementations for testing
    static class MockEmbeddingCacheStore implements EmbeddingCacheStore {
        @Override
        public java.util.List<Double> getCachedEmbedding(String text) {
            return null;
        }
        
        @Override
        public void storeEmbedding(String id, String text, java.util.List<Double> embedding) {
            // Mock implementation
        }
        
        @Override
        public void storeEmbeddings(java.util.List<com.kingname.embeddingstoremanager.vo.CachedEmbeddingDocument> documents) {
            // Mock implementation
        }
    }
    
    static class MockEmbeddingGenerator implements EmbeddingGenerator {
        @Override
        public java.util.List<Double> generateEmbedding(String text) {
            return java.util.Arrays.asList(0.1, 0.2, 0.3);
        }
    }
}
