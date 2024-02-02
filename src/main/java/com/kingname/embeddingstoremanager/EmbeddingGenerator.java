package com.kingname.embeddingstoremanager;

import com.kingname.embeddingstoremanager.exception.EmbeddingGeneratorException;

import java.util.List;

public interface EmbeddingGenerator {
    List<Double> generateEmbedding(String text) throws EmbeddingGeneratorException;
}
