package com.kingname.embeddingstoremanager;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.kingname.embeddingstoremanager.exception.ElasticSearchClientException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;


public class ElasticSearchClientBuilder {

    public static ElasticsearchClient build(EmbeddingCacheManagerConfig ecmConfig) throws ElasticSearchClientException {
        try {
            return new ElasticsearchClient(getTransport(ecmConfig));
        }catch (Exception e) {
            throw new ElasticSearchClientException(e.getMessage(), e.getCause());
        }
    }

    private static ElasticsearchTransport getTransport(EmbeddingCacheManagerConfig ecmConfig) {
        return new RestClientTransport(getRestClient(getHttpHosts(ecmConfig)), new JacksonJsonpMapper());
    }

    private static RestClient getRestClient(HttpHost[] httpHosts) {
        return RestClient.builder(httpHosts)
                .setRequestConfigCallback(getRequestConfigCallBack())
                .build();
    }

    private  static RestClientBuilder.RequestConfigCallback getRequestConfigCallBack() {
        return requestConfigBuilder -> requestConfigBuilder
                .setConnectionRequestTimeout(2000)
                .setSocketTimeout(3000);
    }

    private static HttpHost[] getHttpHosts(EmbeddingCacheManagerConfig ecmConfig) {
        return ecmConfig.getElasticSearchCacheHosts()
                .stream()
                .filter(StringUtils::isNotBlank)
                .map(host -> new HttpHost(host, ecmConfig.getElasticSearchCachePort()))
                .toArray(HttpHost[]::new);
    }
}
