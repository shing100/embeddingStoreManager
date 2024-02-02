package com.kingname.embeddingstoremanager;

import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class RestEmbeddingGenerator implements EmbeddingGenerator {

    private final Gson gson = new Gson();
    private final EmbeddingCacheManagerConfig ecmConfig;

    public RestEmbeddingGenerator(EmbeddingCacheManagerConfig ecmConfig) {
        this.ecmConfig = ecmConfig;
    }

    @Override
    public List<Double> generateEmbedding(String text) throws RestEmbeddingGeneratorException {
        try {
            HttpPost httpPost = new HttpPost(this.ecmConfig.getEmbeddingApiUrl());
            Map<String, String> body = new HashMap<>();
            body.put("input", text);
            body.put("model", this.ecmConfig.getModelName());
            StringEntity entity = new StringEntity(gson.toJson(body), "UTF-8");
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response = client.execute(httpPost);
            Scanner sc = new Scanner(response.getEntity().getContent());
            StringBuffer responseBody = new StringBuffer();
            while(sc.hasNext()) {
                responseBody.append(sc.nextLine());
            }
            EmbeddingResponse embeddingResponse = gson.fromJson(responseBody.toString(), EmbeddingResponse.class);
            return embeddingResponse.getData().get(0).getEmbedding();
        } catch (Exception e) {
            throw new RestEmbeddingGeneratorException(e.getMessage(), e.getCause());
        }
    }
}
