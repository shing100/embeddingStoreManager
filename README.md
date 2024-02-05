## 임베딩 스토어 라이브러리

```
### gradle 사용시
repositories {
    maven { url 'https://jitpack.io' }
    mavenCentral()
}

dependencies {
    implementation 'com.github.shing100:embeddingStoreManager:1.0.3'
}
```

### 라이브러리 설치 후
```java
    @Bean
    public EmbeddingCacheManager embeddingCacheManager() throws EmbeddingCacheManagerException {
        return new EmbeddingCacheManager(EmbeddingCacheManagerConfig.builder() 
                .elasticSearchCacheHosts(this.arguments.getEsServers()) # es 서버 hosts
                .elasticSearchCachePort(this.arguments.getEsPort()) # es 서버 port
                .elasticSearchCacheAliasName(this.arguments.getEsEmbeddingCacheAliasName()) # es에 저장할 index명
                .modelName(this.arguments.getEmbeddingModelName()) # 임베딩 모델명
                .embeddingApiUrl(this.arguments.getEmbeddingApiUrl()) # 임베딩 API 주소
                .build());
    }
```

