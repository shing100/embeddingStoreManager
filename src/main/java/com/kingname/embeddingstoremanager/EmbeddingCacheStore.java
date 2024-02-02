package com.kingname.embeddingstoremanager;

import com.kingname.embeddingstoremanager.exception.EmbeddingCacheStoreException;
import com.kingname.embeddingstoremanager.vo.CachedEmbeddingDocument;

import java.util.List;


public interface EmbeddingCacheStore {
    List<Double> getCachedEmbedding(String text) throws EmbeddingCacheStoreException;
    default void storeEmbedding(String text, List<Double> embedding) throws EmbeddingCacheStoreException {
        this.storeEmbedding(null, text, embedding);
    }
    void storeEmbedding(String id, String text, List<Double> embedding) throws EmbeddingCacheStoreException;
    default void storeEmbedding(CachedEmbeddingDocument document) throws EmbeddingCacheStoreException {
        this.storeEmbedding(document.getText(), document.getEmbedding());
    }
    void storeEmbeddings(List<CachedEmbeddingDocument> documents) throws EmbeddingCacheStoreException;
}
