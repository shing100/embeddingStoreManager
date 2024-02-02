package com.kingname.embeddingstoremanager.exception;

public class ElasticSearchClientException extends EmbeddingCacheManagerException {
    public ElasticSearchClientException(Throwable cause) {
        super(cause);
    }
    public ElasticSearchClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
