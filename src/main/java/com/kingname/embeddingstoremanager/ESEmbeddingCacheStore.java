package com.kingname.embeddingstoremanager;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.elasticsearch.indices.update_aliases.AddAction;
import com.kingname.embeddingstoremanager.exception.ElasticSearchClientException;
import com.kingname.embeddingstoremanager.exception.EmbeddingCacheStoreException;
import com.kingname.embeddingstoremanager.vo.CachedEmbeddingDocument;
import com.kingname.embeddingstoremanager.vo.EsCachedEmbeddingDocument;
import lombok.SneakyThrows;

import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ESEmbeddingCacheStore implements EmbeddingCacheStore {

    private final EmbeddingCacheManagerConfig ecmConfig;
    private final ElasticsearchClient esClient;
    private final HashGenerator hashGenerator;

    public ESEmbeddingCacheStore(EmbeddingCacheManagerConfig ecmConfig) throws ElasticSearchClientException {
        this.ecmConfig = ecmConfig;
        this.esClient = ElasticSearchClientBuilder.build(ecmConfig);
        this.hashGenerator = new HashGenerator();
        init(ecmConfig);
    }

    public void init(EmbeddingCacheManagerConfig ecmConfig) throws ElasticSearchClientException {
        createEmbeddingCacheIndexIfNotExists(ecmConfig);
    }

    private void createEmbeddingCacheIndexIfNotExists(EmbeddingCacheManagerConfig ecmConfig) throws ElasticSearchClientException {
        String aliasName = ecmConfig.getElasticSearchCacheAliasName();
        String indexName = getIndexName(aliasName);
        CreateIndexResponse createIndexResponse = createIndexIfNotExists(indexName, aliasName);
        if(Objects.isNull(createIndexResponse)) return; // 이미 존재함.
        if(createIndexResponse.acknowledged()) { // 생성 완료
            PutAliasResponse putAliasResponse = addAlias(indexName, aliasName, true);
            if(!putAliasResponse.acknowledged()) {
                throw new ElasticSearchClientException(new IllegalStateException(aliasName + " alias 생성에 실패했습니다."));
            }
        }else {
            throw new ElasticSearchClientException(new IllegalStateException(indexName + " index 생성에 실패했습니다."));
        }
    }

    private String getIndexName(String aliasName) {
        String nowDate = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMM"));
        return String.format("%s-%s", aliasName, nowDate);
    }

    private CreateIndexResponse createIndexIfNotExists(String indexName, String aliasName) throws ElasticSearchClientException {
        if(!existsIndex(aliasName)) {
            String mappings = readStringFromClassPath("/mappings.json");
            String settings = readStringFromClassPath("/settings.json");
            return createIndex(indexName, mappings, settings);
        }
        return null;
    }

    private String readStringFromClassPath(String path) throws ElasticSearchClientException {
        try(InputStream in = getClass().getResourceAsStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            StringBuffer stringBuffer = new StringBuffer();
            do {
                line = reader.readLine();
                stringBuffer.append(line);
            }while (line != null);
            return stringBuffer.toString();
        } catch (IOException e) {
            throw new ElasticSearchClientException(e.getMessage(), e.getCause());
        }
    }

    private boolean existsIndex(String indexName) throws ElasticSearchClientException {
        try {
            ExistsRequest existsRequest = new ExistsRequest.Builder().index(indexName).build();
            return this.esClient.indices().exists(existsRequest).value();
        }catch (Exception e) {
            throw new ElasticSearchClientException(e.getMessage(), e.getCause());
        }
    }

    private CreateIndexResponse createIndex(String indexName, String mappings, String settings) throws ElasticSearchClientException {
        try {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
                    .index(indexName)
                    .mappings(new TypeMapping.Builder()
                            .withJson(new ByteArrayInputStream(mappings.getBytes()))
                            .build())
                    .settings(new IndexSettings.Builder()
                            .withJson(new ByteArrayInputStream(settings.getBytes()))
                            .build())
                    .masterTimeout(new Time.Builder().time("5m").build())
                    .build();
            return this.esClient.indices().create(createIndexRequest);
        }catch (Exception e) {
            throw new ElasticSearchClientException(e.getMessage(), e.getCause());
        }
    }

    private GetAliasResponse getAlias(String aliasName) throws ElasticSearchClientException {
        try {
            return this.esClient.indices().getAlias(new GetAliasRequest.Builder()
                            .index(aliasName)
                            .build());
        }catch (Exception e) {
            throw new ElasticSearchClientException(e.getMessage(), e.getCause());
        }
    }

    private DeleteAliasResponse deleteAlias(String aliasName) throws ElasticSearchClientException {
        try {
            List<String> indexNames = getIndexNamesFromAlias(aliasName);
            indexNames = indexNames.stream().filter(str -> !str.endsWith("base")).collect(Collectors.toList());
            if(indexNames.size() <= this.ecmConfig.getRetentionMonth()) return null; // 삭제할게 없음.
            int count = indexNames.size() - this.ecmConfig.getRetentionMonth();
            List<String> deleteIndexNames = indexNames.subList(0, count);
            return this.esClient.indices().deleteAlias(new DeleteAliasRequest.Builder()
                    .index(deleteIndexNames)
                    .name(aliasName)
                    .build());
        }catch (Exception e) {
            throw new ElasticSearchClientException(e.getMessage(), e.getCause());
        }
    }

    private PutAliasResponse addAlias(String indexName, String aliasName, boolean isWriteIndex) throws ElasticSearchClientException {
        try {
            return this.esClient.indices().putAlias(new PutAliasRequest.Builder()
                    .index(indexName)
                    .name(aliasName)
                    .isWriteIndex(isWriteIndex)
                    .build());
        }catch (Exception e) {
            throw new ElasticSearchClientException(e.getMessage(), e.getCause());
        }
    }

    public List<Double> getCachedEmbedding(String text) throws EmbeddingCacheStoreException {
        try {
            String hash = this.hashGenerator.getHash(text);
            SearchRequest searchRequest = getHashSearchRequest(hash);
            List<EsCachedEmbeddingDocument> cachedList = search(searchRequest);
            return cachedList.stream()
                    .filter(doc -> text.equalsIgnoreCase(doc.getText()))
                    .filter(doc -> Objects.nonNull(doc.getEmbedding()))
                    .filter(doc -> doc.getEmbedding().size() > 0)
                    .findFirst()
                    .map(EsCachedEmbeddingDocument::getEmbedding)
                    .orElse(null);
        } catch (Exception e) {
            throw new EmbeddingCacheStoreException(e.getMessage(), e.getCause());
        }
    }

    @SneakyThrows
    private List<EsCachedEmbeddingDocument> search(SearchRequest searchRequest) {
        return this.esClient.search(searchRequest, EsCachedEmbeddingDocument.class)
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    private SearchRequest getHashSearchRequest(String hash) {
        return new SearchRequest.Builder()
                .index(this.ecmConfig.getElasticSearchCacheAliasName())
                .query(getQuery(hash))
                .size(1)
                .build();
    }

    private IndexRequest<EsCachedEmbeddingDocument> getIndexRequest(EsCachedEmbeddingDocument document) {
        return new IndexRequest.Builder<EsCachedEmbeddingDocument>()
                .index(this.ecmConfig.getElasticSearchCacheAliasName())
                .id(document.getId())
                .document(document)
                .build();
    }

    private Query getQuery(String hash) {
        return new BoolQuery.Builder()
                .filter(Arrays.asList(
                        new TermQuery.Builder()
                                .field("hash")
                                .value(hash)
                                .build()
                                ._toQuery(),
                        new TermQuery.Builder()
                                .field("has_embedding")
                                .value(true)
                                .build()
                                ._toQuery()
                )).build()
                ._toQuery();
    }

    @Override
    public void storeEmbedding(String id, String text, List<Double> embedding) throws EmbeddingCacheStoreException {
        try {
            EsCachedEmbeddingDocument esCachedEmbeddingDocument = buildEmbeddingDocument(id, text, embedding);
            this.esClient.index(getIndexRequest(esCachedEmbeddingDocument));
        } catch (Exception e) {
            throw new EmbeddingCacheStoreException(e.getMessage(), e.getCause());
        }
    }

    @SneakyThrows
    private EsCachedEmbeddingDocument buildEmbeddingDocument(String id, String text, List<Double> embedding) {
        String hash = this.hashGenerator.getHash(text);
        return EsCachedEmbeddingDocument.builder()
                .id(id)
                .embedding(embedding)
                .text(text)
                .hash(hash)
                .has_embedding(Objects.nonNull(embedding))
                .build();
    }

    @SneakyThrows
    private EsCachedEmbeddingDocument buildEmbeddingDocument(String text, List<Double> embedding) {
        return this.buildEmbeddingDocument(null, text, embedding);
    }

    @Override
    public void storeEmbeddings(List<CachedEmbeddingDocument> documents) throws EmbeddingCacheStoreException {
        try {
            List<BulkOperation> bulkOperations = documents.stream()
                    .map(doc -> buildEmbeddingDocument(((EsCachedEmbeddingDocument) doc).getId(), doc.getText(), doc.getEmbedding()))
                    .map(doc -> new IndexOperation.Builder<EsCachedEmbeddingDocument>()
                            .id(doc.getId())
                            .document(doc)
                            .build())
                    .map(indexOperation -> new BulkOperation.Builder()
                            .index(indexOperation)
                            .build())
                    .collect(Collectors.toList());
            this.esClient.bulk(new BulkRequest.Builder()
                    .index(this.ecmConfig.getElasticSearchCacheAliasName())
                    .operations(bulkOperations)
                    .timeout(new Time.Builder().time("5m").build())
                    .build());
        } catch (Exception e) {
            throw new EmbeddingCacheStoreException(e.getMessage(), e.getCause());
        }
    }

    public boolean rollUpEmbeddingIndex() throws ElasticSearchClientException {
        try {
            String aliasName = this.ecmConfig.getElasticSearchCacheAliasName();
            String indexName = getIndexName(aliasName);
            CreateIndexResponse createIndexResponse = createIndexIfNotExists(indexName, indexName);
            if(Objects.isNull(createIndexResponse))
                return true; // 이미 존재
            if(!createIndexResponse.acknowledged())
                throw new ElasticSearchClientException(new IllegalStateException(indexName + " index 생성에 실패했습니다."));
            PutAliasResponse  putAliasResponse = addAlias(indexName, aliasName, false);
            if(!putAliasResponse.acknowledged())
                throw new ElasticSearchClientException(new IllegalStateException(indexName + " alias 추가에 실패했습니다."));
            UpdateAliasesResponse updateAliasesResponse = updateWriteIndex(indexName, aliasName);
            if(!updateAliasesResponse.acknowledged())
                throw new ElasticSearchClientException(new IllegalStateException(indexName + " alias 업데이트에 실패했습니다."));
            DeleteAliasResponse deleteAliasResponse = deleteAlias(aliasName);
            if(Objects.nonNull(deleteAliasResponse) && !deleteAliasResponse.acknowledged())
                throw new ElasticSearchClientException(new IllegalStateException(indexName + "의 alias 삭제에 실패했습니다."));
            return true;
        } catch (Exception e) {
            throw new ElasticSearchClientException(e.getMessage(), e.getCause());
        }
    }

    private UpdateAliasesResponse updateWriteIndex(String writeIndexName, String aliasName) throws IOException, ElasticSearchClientException {
        List<String> indexNames = getIndexNamesFromAlias(aliasName);
        List<Action> actions = indexNames.stream()
                .map(indexName -> new AddAction.Builder()
                        .index(indexName)
                        .alias(aliasName)
                        .isWriteIndex(writeIndexName.equals(indexName))
                        .build())
                .map(action -> new Action.Builder()
                        .add(action)
                        .build())
                .collect(Collectors.toList());
        UpdateAliasesRequest updateAliasesRequest = new UpdateAliasesRequest.Builder().actions(actions).build();
        return this.esClient.indices().updateAliases(updateAliasesRequest);
    }

    private List<String> getIndexNamesFromAlias(String aliasName) throws ElasticSearchClientException {
        GetAliasResponse getAliasResponse = getAlias(aliasName);
        return getAliasResponse.result().keySet().stream().sorted().collect(Collectors.toList());
    }
}
