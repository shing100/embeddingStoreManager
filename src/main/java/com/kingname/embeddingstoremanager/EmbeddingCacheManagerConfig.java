package com.kingname.embeddingstoremanager;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Builder
@Getter
@ToString
public class EmbeddingCacheManagerConfig {
    private final List<String> elasticSearchCacheHosts;
    private final Integer elasticSearchCachePort;
    private final String elasticSearchCacheAliasName;
    @Builder.Default
    private final Integer retentionMonth = 3;
    private final String modelName;
    private final String embeddingApiUrl;
    @Builder.Default
    private final Integer maxLength = 3_000;
}
