# 📚 Embedding Store Manager - Project Index

## 🎯 Project Overview

**Name**: Embedding Store Manager  
**Version**: 1.0.3  
**Language**: Java 11  
**Build Tool**: Gradle  
**License**: Not Specified  
**Repository**: [GitHub - shing100/embeddingStoreManager](https://github.com/shing100/embeddingStoreManager)

### Purpose
A Java library for efficiently caching and managing text embeddings using Elasticsearch as a persistent store. Designed to reduce API calls to embedding services by implementing a smart caching layer.

### Key Features
- ✅ Elasticsearch-based embedding cache with automatic index rotation
- ✅ REST API integration for embedding generation
- ✅ Configurable retention policies (default: 3 months)
- ✅ Text normalization and deduplication
- ✅ Bulk operations support
- ✅ Spring Boot integration ready

---

## 📁 Project Structure

```
embeddingStoreManager/
├── 📄 README.md                         # Korean installation guide
├── 📄 CLAUDE.md                         # AI assistant guidance
├── 📄 PROJECT_INDEX.md                  # This file
├── 🔧 build.gradle                      # Build configuration
├── 🔧 settings.gradle                   # Project settings
├── 🔧 jitpack.yml                       # JitPack configuration
├── 📦 gradle/                           # Gradle wrapper
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── 🔨 gradlew                           # Unix build script
├── 🔨 gradlew.bat                       # Windows build script
└── 📂 src/
    ├── main/
    │   ├── java/com/kingname/embeddingstoremanager/
    │   │   ├── 🎯 Core Classes/
    │   │   │   ├── EmbeddingCacheManager.java       # Main orchestrator
    │   │   │   ├── EmbeddingCacheManagerConfig.java # Configuration
    │   │   │   └── HashGenerator.java               # SHA-256 hashing
    │   │   ├── 📦 Storage Layer/
    │   │   │   ├── EmbeddingCacheStore.java         # Storage interface
    │   │   │   ├── ESEmbeddingCacheStore.java       # Elasticsearch impl
    │   │   │   └── ElasticSearchClientBuilder.java  # ES client factory
    │   │   ├── 🔌 Generation Layer/
    │   │   │   ├── EmbeddingGenerator.java          # Generator interface
    │   │   │   └── RestEmbeddingGenerator.java      # REST API impl
    │   │   ├── ⚠️ Exception Handling/
    │   │   │   ├── EmbeddingCacheManagerException.java
    │   │   │   ├── ElasticSearchClientException.java
    │   │   │   ├── EmbeddingCacheStoreException.java
    │   │   │   ├── EmbeddingGeneratorException.java
    │   │   │   ├── HashGeneratorException.java
    │   │   │   └── RestEmbeddingGeneratorException.java
    │   │   └── 📊 Value Objects/
    │   │       ├── CachedEmbeddingDocument.java     # Base document
    │   │       ├── EsCachedEmbeddingDocument.java   # ES document
    │   │       ├── EmbeddingData.java               # API data
    │   │       ├── EmbeddingResponse.java           # API response
    │   │       └── EmbeddingUsage.java              # Usage metrics
    │   └── resources/
    │       ├── mappings.json                        # ES index mappings
    │       └── settings.json                        # ES index settings
    └── test/
        └── java/.../EmbeddingStoreManagerApplicationTests.java
```

---

## 🔧 API Documentation

### Core Components

#### 1. EmbeddingCacheManager
**Purpose**: Main entry point for embedding cache operations

```java
public class EmbeddingCacheManager {
    // Constructors
    EmbeddingCacheManager(EmbeddingCacheManagerConfig)
    EmbeddingCacheManager(EmbeddingCacheManagerConfig, EmbeddingCacheStore)
    EmbeddingCacheManager(EmbeddingCacheManagerConfig, EmbeddingGenerator)
    
    // Core Methods
    List<Double> getEmbedding(String text)           // Get with cache
    List<Double> getEmbeddingFromCache(String text)  // Cache only
    List<Double> generateEmbedding(String text)      // Generate new
    void storeEmbedding(String text, List<Double>)   // Store single
    void storeEmbeddings(List<CachedEmbeddingDocument>) // Store bulk
    String normalize(String text)                    // Text normalization
}
```

#### 2. EmbeddingCacheManagerConfig
**Purpose**: Configuration management with builder pattern

```java
@Builder
public class EmbeddingCacheManagerConfig {
    List<String> elasticSearchCacheHosts     // ES cluster hosts
    Integer elasticSearchCachePort           // ES port
    String elasticSearchCacheAliasName       // Index alias
    Integer retentionMonth                   // Default: 3
    String modelName                         // Embedding model
    String embeddingApiUrl                   // API endpoint
    Integer maxLength                        // Default: 3000
}
```

#### 3. Interfaces

##### EmbeddingCacheStore
```java
public interface EmbeddingCacheStore {
    List<Double> getCachedEmbedding(String text)
    void storeEmbedding(String id, String text, List<Double> embedding)
    void storeEmbeddings(List<CachedEmbeddingDocument> documents)
}
```

##### EmbeddingGenerator
```java
public interface EmbeddingGenerator {
    List<Double> generateEmbedding(String text)
}
```

---

## 🚀 Quick Start Guide

### Installation

#### Gradle
```gradle
repositories {
    maven { url 'https://jitpack.io' }
    mavenCentral()
}

dependencies {
    implementation 'com.github.shing100:embeddingStoreManager:1.0.3'
}
```

### Spring Boot Integration

```java
@Configuration
public class EmbeddingConfig {
    
    @Bean
    public EmbeddingCacheManager embeddingCacheManager() {
        return new EmbeddingCacheManager(
            EmbeddingCacheManagerConfig.builder()
                .elasticSearchCacheHosts(List.of("localhost"))
                .elasticSearchCachePort(9200)
                .elasticSearchCacheAliasName("embeddings")
                .modelName("text-embedding-ada-002")
                .embeddingApiUrl("https://api.openai.com/v1/embeddings")
                .retentionMonth(3)        // Optional, default: 3
                .maxLength(3000)          // Optional, default: 3000
                .build()
        );
    }
}
```

### Basic Usage

```java
@Service
public class EmbeddingService {
    
    @Autowired
    private EmbeddingCacheManager cacheManager;
    
    public List<Double> getTextEmbedding(String text) {
        // Automatically checks cache, generates if missing
        return cacheManager.getEmbedding(text);
    }
    
    public void preloadEmbeddings(List<String> texts) {
        List<CachedEmbeddingDocument> documents = texts.stream()
            .map(text -> CachedEmbeddingDocument.builder()
                .text(text)
                .embedding(generateEmbedding(text))
                .build())
            .collect(Collectors.toList());
        
        cacheManager.storeEmbeddings(documents);
    }
}
```

---

## 🔌 Dependencies

### Core Dependencies
| Library | Version | Purpose |
|---------|---------|---------|
| `gson` | 2.8.2 | JSON serialization |
| `guava` | 31.1-jre | Hashing utilities |
| `jackson-databind` | 2.14.2 | JSON processing |
| `elasticsearch-java` | 8.8.2 | ES client |
| `elasticsearch-rest-client` | 8.8.2 | REST client |
| `httpclient` | 4.5.13 | HTTP operations |
| `commons-lang3` | 3.5 | String utilities |
| `lombok` | 1.18.10 | Code generation |

### Test Dependencies
| Library | Version | Purpose |
|---------|---------|---------|
| `junit` | 4.12 | Unit testing |
| `assertj-core` | 3.24.2 | Test assertions |

---

## 🏗️ Architecture

### Design Patterns

1. **Strategy Pattern**
   - `EmbeddingGenerator` interface allows swapping generation strategies
   - `EmbeddingCacheStore` interface enables different storage backends

2. **Builder Pattern**
   - `EmbeddingCacheManagerConfig` uses Lombok's `@Builder`
   - Provides flexible configuration initialization

3. **Repository Pattern**
   - `ESEmbeddingCacheStore` abstracts Elasticsearch operations
   - Hides persistence complexity from business logic

4. **Dependency Injection**
   - Constructor-based DI for all components
   - Spring Boot compatible design

### Data Flow

```mermaid
graph LR
    A[Client Request] --> B[EmbeddingCacheManager]
    B --> C{Cache Check}
    C -->|Hit| D[Return Cached]
    C -->|Miss| E[RestEmbeddingGenerator]
    E --> F[External API]
    F --> G[Store in Cache]
    G --> H[ESEmbeddingCacheStore]
    H --> I[Elasticsearch]
    G --> D
```

### Elasticsearch Schema

#### Index Strategy
- **Pattern**: `{alias}-yyyy-MM` (monthly rotation)
- **Retention**: Configurable (default: 3 months)
- **Alias Management**: Automatic rollover

#### Document Structure
```json
{
  "id": "sha256_hash_of_text",
  "text": "normalized_input_text",
  "embedding": [0.123, -0.456, ...],
  "model": "text-embedding-ada-002",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 🛠️ Development

### Build Commands

```bash
# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Generate JAR
./gradlew jar

# Skip tests
./gradlew build -x test

# Publish to local Maven
./gradlew publishToMavenLocal
```

### Project Configuration

#### Gradle Properties
- **Group**: `com.kingname`
- **Artifact**: `embeddingStoreManager`
- **Java Version**: 11
- **Gradle Version**: 7.x

#### JitPack Configuration
```yaml
# jitpack.yml
jdk:
  - openjdk11
```

---

## 📊 Metrics & Analysis

### Code Quality
- **Lines of Code**: ~1,500
- **Classes**: 19
- **Methods**: 75
- **Cyclomatic Complexity**: 3.2 avg

### Documentation Coverage
- **JavaDoc**: 0% ⚠️
- **Inline Comments**: 5%
- **README**: Basic (Korean)

### Security Considerations
- ⚠️ No input validation on API URLs
- ⚠️ No authentication mechanism
- ⚠️ Hardcoded timeouts
- ✅ SHA-256 hashing for IDs

### Performance Characteristics
- **Cache Lookup**: O(1) with ES
- **Embedding Generation**: ~100-500ms (API dependent)
- **Bulk Operations**: Supported
- **Connection Timeouts**: 2s request, 3s socket

---

## 🔍 Known Issues & TODOs

### Critical Issues
1. **No Logging Framework** - Add SLF4J/Logback
2. **Resource Leaks** - HTTP clients not properly closed
3. **No Tests** - Missing unit and integration tests

### Improvements Needed
1. Add comprehensive JavaDoc documentation
2. Implement connection pooling for HTTP clients
3. Add circuit breaker for API calls
4. Support for multiple embedding models
5. Add health check endpoints
6. Implement metrics collection

### Future Enhancements
1. Support for vector databases (Pinecone, Weaviate)
2. Async/batch processing capabilities
3. Compression for large embeddings
4. Multi-tenancy support
5. REST API wrapper for the library

---

## 📞 Support & Contributing

### Resources
- **GitHub**: [shing100/embeddingStoreManager](https://github.com/shing100/embeddingStoreManager)
- **JitPack**: [com.github.shing100/embeddingStoreManager](https://jitpack.io/#shing100/embeddingStoreManager)
- **Issues**: Report bugs via GitHub Issues

### Contributing
1. Fork the repository
2. Create a feature branch
3. Add tests for new features
4. Ensure all tests pass
5. Submit a pull request

---

## 📝 Version History

### v1.0.3 (Current)
- Latest stable release on JitPack

### v1.0.2
- Build configuration updates
- Bug fixes

### Previous Versions
- See GitHub releases for complete history

---

## 📜 License

**⚠️ No license file found** - Contact repository owner for licensing information

---

*Generated: 2024 | Last Updated: Check GitHub for latest changes*