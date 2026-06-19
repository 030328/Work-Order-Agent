package com.wo.agent.rag;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.collection.*;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class MilvusClientWrapper {

    @Value("${milvus.host:192.168.213.100}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${milvus.collection:wo_knowledge_vectors}")
    private String collectionName;

    private MilvusServiceClient client;
    private boolean connected = false;

    @PostConstruct
    public void init() {
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .build();
            client = new MilvusServiceClient(connectParam);

            HasCollectionParam checkParam = HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();
            R<Boolean> result = client.hasCollection(checkParam);
            connected = result.getData();
            log.info("Milvus connected to {}:{}, collection '{}' exists: {}", host, port, collectionName, connected);

        } catch (Exception e) {
            log.error("Failed to connect to Milvus at {}:{}", host, port, e);
        }
    }

    public void insert(String content, float[] vector, String sourceType, String sourceId) {
        if (!connected) {
            log.warn("Milvus not connected, skip insert");
            return;
        }

        try {
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("content", List.of(content)));
            fields.add(new InsertParam.Field("vector", List.of(toFloatList(vector))));
            fields.add(new InsertParam.Field("source_type", List.of(sourceType)));
            fields.add(new InsertParam.Field("source_id", List.of(sourceId)));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            client.insert(insertParam);
            log.debug("Inserted document into Milvus");
        } catch (Exception e) {
            log.error("Failed to insert into Milvus", e);
        }
    }

    public List<Map<String, Object>> search(float[] queryVector, int topK) {
        if (!connected) {
            log.warn("Milvus not connected, skip search");
            return List.of();
        }

        try {
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withVectors(List.of(toFloatList(queryVector)))
                    .withVectorFieldName("vector")
                    .withTopK(topK)
                    .withMetricType(MetricType.COSINE)
                    .withOutFields(List.of("content", "source_type", "source_id"))
                    .withConsistencyLevel(ConsistencyLevelEnum.BOUNDED)
                    .build();

            R<io.milvus.grpc.SearchResults> searchResult = client.search(searchParam);
            SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResult.getData().getResults());

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (SearchResultsWrapper.IDScore idScore : wrapper.getIDScore(0)) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", idScore.getLongID());
                item.put("score", idScore.getScore());
                item.put("content", idScore.get("content"));
                item.put("source_type", idScore.get("source_type"));
                item.put("source_id", idScore.get("source_id"));
                resultList.add(item);
            }
            return resultList;
        } catch (Exception e) {
            log.error("Failed to search Milvus", e);
            return List.of();
        }
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }
}
