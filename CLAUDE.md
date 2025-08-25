# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an embedding store management library for caching and managing text embeddings using Elasticsearch. It provides a Java library for efficiently storing, retrieving, and managing embeddings with built-in caching capabilities.

## Build and Development Commands

### Building the Project
```bash
# Build the project
./gradlew build

# Clean and build
./gradlew clean build

# Run tests
./gradlew test

# Run a specific test class
./gradlew test --tests com.kingname.embeddingstoremanager.EmbeddingStoreManagerApplicationTests

# Build without tests
./gradlew build -x test

# Generate JAR for library distribution
./gradlew jar
```

### Publishing to JitPack
```bash
# Publish to local Maven repository for testing
./gradlew publishToMavenLocal

# The library is published via JitPack using GitHub releases
# Current version: 1.0.3 (as per README)
```

## Architecture Overview

### Core Components

1. **EmbeddingCacheManager** - Main entry point that orchestrates caching and generation
   - Manages cache lookups and misses
   - Coordinates between cache store and embedding generator
   - Normalizes text before processing (lowercase, trim, length limit)

2. **ESEmbeddingCacheStore** - Elasticsearch-based cache implementation
   - Handles index creation with monthly rotation
   - Manages alias updates and data retention
   - Uses SHA-256 hashing for document IDs
   - Implements bulk operations for efficiency

3. **RestEmbeddingGenerator** - REST API client for embedding generation
   - Makes HTTP POST requests to embedding API
   - Handles JSON serialization/deserialization
   - Manages API errors and responses

4. **EmbeddingCacheManagerConfig** - Configuration management
   - Uses Lombok Builder pattern
   - Configurable defaults (3-month retention, 3000 char max length)
   - Stores ES hosts, ports, alias names, model info

### Key Design Patterns

- **Strategy Pattern**: Different embedding generators and cache stores can be injected
- **Builder Pattern**: Configuration uses Lombok's @Builder for flexible initialization
- **Repository Pattern**: Cache store abstracts data persistence details
- **Template Method**: Base interfaces define contracts for extensibility

### Data Flow

1. Text input → Normalization (lowercase, trim, max 3000 chars)
2. Check cache using SHA-256 hash of normalized text
3. If cache miss → Generate embedding via REST API
4. Store embedding in Elasticsearch with metadata
5. Return embedding to caller

### Elasticsearch Integration

- **Index Structure**: Monthly indices with alias management (format: `{alias}-yyyy-MM`)
- **Document Structure**: Stores text hash, embedding vector, timestamp, model name
- **Retention**: Configurable retention period (default 3 months)
- **Mappings**: Custom mappings for efficient storage and retrieval (see `resources/mappings.json`)

### Exception Hierarchy

All exceptions extend from specific base exceptions:
- `EmbeddingCacheManagerException` - Top-level configuration errors
- `ElasticSearchClientException` - ES client/connection issues  
- `EmbeddingCacheStoreException` - Cache storage operations
- `EmbeddingGeneratorException` - Embedding generation failures
- `RestEmbeddingGeneratorException` - REST API specific errors
- `HashGeneratorException` - Hashing operation failures

### Integration Usage

The library is designed to be used as a Spring Bean:
```java
@Bean
public EmbeddingCacheManager embeddingCacheManager() {
    return new EmbeddingCacheManager(
        EmbeddingCacheManagerConfig.builder()
            .elasticSearchCacheHosts(hosts)
            .elasticSearchCachePort(port)
            .elasticSearchCacheAliasName(aliasName)
            .modelName(modelName)
            .embeddingApiUrl(apiUrl)
            .build()
    );
}
```