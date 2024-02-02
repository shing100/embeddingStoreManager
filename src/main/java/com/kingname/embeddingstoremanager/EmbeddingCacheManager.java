package com.kingname.embeddingstoremanager;

import com.kingname.embeddingstoremanager.exception.ElasticSearchClientException;
import com.kingname.embeddingstoremanager.exception.EmbeddingCacheManagerException;
import com.kingname.embeddingstoremanager.exception.EmbeddingCacheStoreException;
import com.kingname.embeddingstoremanager.exception.EmbeddingGeneratorException;
import com.kingname.embeddingstoremanager.vo.CachedEmbeddingDocument;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EmbeddingCacheManager {

    private final EmbeddingCacheStore embeddingCacheStore;
    private final EmbeddingGenerator openAIEmbeddingGenerator;
    private final EmbeddingCacheManagerConfig embeddingCacheManagerConfig;

    public EmbeddingCacheManager(EmbeddingCacheManagerConfig ecmConfig) throws EmbeddingCacheManagerException {
        this(ecmConfig, new ESEmbeddingCacheStore(ecmConfig), new RestEmbeddingGenerator(ecmConfig));
    }

    public EmbeddingCacheManager(EmbeddingCacheManagerConfig ecmConfig, EmbeddingCacheStore esEmbeddingCacheStore) {
        this(ecmConfig, esEmbeddingCacheStore, new RestEmbeddingGenerator(ecmConfig));
    }

    public EmbeddingCacheManager(EmbeddingCacheManagerConfig ecmConfig, EmbeddingGenerator openAIEmbeddingGenerator) throws ElasticSearchClientException {
        this(ecmConfig, new ESEmbeddingCacheStore(ecmConfig), openAIEmbeddingGenerator);
    }

    public EmbeddingCacheManager(EmbeddingCacheManagerConfig embeddingCacheManagerConfig, EmbeddingCacheStore esEmbeddingCacheStore, EmbeddingGenerator openAIEmbeddingGenerator) {
        this.embeddingCacheStore = esEmbeddingCacheStore;
        this.openAIEmbeddingGenerator = openAIEmbeddingGenerator;
        this.embeddingCacheManagerConfig = embeddingCacheManagerConfig;
    }

    public List<Double> getEmbedding(String text) throws EmbeddingCacheStoreException, EmbeddingGeneratorException {
        List<Double> embedding = getEmbeddingFromCache(text);
        if(Objects.isNull(embedding)) {
            embedding = generateEmbedding(text);
            storeEmbedding(text, embedding);
        }
        return embedding;
    }

    public List<Double> getEmbeddingFromCache(String text) throws EmbeddingCacheStoreException {
        return embeddingCacheStore.getCachedEmbedding(normalize(text));
    }

    public List<Double> generateEmbedding(String text) throws EmbeddingGeneratorException {
        return openAIEmbeddingGenerator.generateEmbedding(normalize(text));
    }

    public void storeEmbedding(String text, List<Double> embedding) throws EmbeddingCacheStoreException {
        this.embeddingCacheStore.storeEmbedding(normalize(text), embedding);
    }

    public void storeEmbedding(CachedEmbeddingDocument document) throws EmbeddingCacheStoreException {
        this.embeddingCacheStore.storeEmbedding(document);
    }

    public void storeEmbeddings(List<CachedEmbeddingDocument> documents) throws EmbeddingCacheStoreException {
        this.embeddingCacheStore.storeEmbeddings(documents);
    }

    public String normalize(String text) {
        return text.substring(0, Math.min(text.length(), embeddingCacheManagerConfig.getMaxLength()))
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public EmbeddingCacheStore getEmbeddingCacheStore() {
        return this.embeddingCacheStore;
    }

    public EmbeddingGenerator getOpenAIEmbeddingGenerator() {
        return this.openAIEmbeddingGenerator;
    }
}
