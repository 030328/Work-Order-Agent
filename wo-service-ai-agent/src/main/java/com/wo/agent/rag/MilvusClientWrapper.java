package com.wo.agent.rag;

import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DescribeIndexParam;
import io.milvus.param.index.DropIndexParam;
import io.milvus.param.partition.LoadPartitionsParam;
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

    @PostConstruct
    public void init() {
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .build();
            client = new MilvusServiceClient(connectParam);
            log.info("Milvus client created for {}:{}", host, port);

            createCollectionIfNotExists();
            loadCollection();

        } catch (Exception e) {
            log.error("Failed to initialize Milvus", e);
        }
    }

    private void createCollectionIfNotExists() {
        try {
            // Check if collection exists
            HasCollectionParam checkParam = HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();
            R<Boolean> result = client.hasCollection(checkParam);

            if (result.getData()) {
                log.info("Collection '{}' already exists", collectionName);
                return;
            }

            // Build schema
            FieldType idField = FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(true)
                    .build();

            FieldType contentField = FieldType.newBuilder()
                    .withName("content")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(4000)
                    .build();

            FieldType vectorField = FieldType.newBuilder()
                    .withName("vector")
                    .withDataType(DataType.FloatVector)
                    .withDimension(1024)
                    .build();

            FieldType sourceTypeField = FieldType.newBuilder()
                    .withName("source_type")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(50)
                    .build();

            FieldType sourceIdField = FieldType.newBuilder()
                    .withName("source_id")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(100)
                    .build();

            FieldType verifiedField = FieldType.newBuilder()
                    .withName("verified")
                    .withDataType(DataType.Int64)
                    .build();

            FieldType likeCountField = FieldType.newBuilder()
                    .withName("like_count")
                    .withDataType(DataType.Int64)
                    .build();

            // Create collection
            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withShardsNum(1)
                    .addFieldType(idField)
                    .addFieldType(contentField)
                    .addFieldType(vectorField)
                    .addFieldType(sourceTypeField)
                    .addFieldType(sourceIdField)
                    .addFieldType(verifiedField)
                    .addFieldType(likeCountField)
                    .build();

            R<io.milvus.param.RpcStatus> createResult = client.createCollection(createParam);
            log.info("Collection '{}' created: {}", collectionName, createResult.getStatus());

            // Create index for vector field
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName("vector")
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"nlist\":128}")
                    .build();

            client.createIndex(indexParam);
            log.info("Index created for collection '{}'", collectionName);

        } catch (Exception e) {
            log.error("Failed to create collection", e);
        }
    }

    private void loadCollection() {
        try {
            LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();
            client.loadCollection(loadParam);
            log.info("Collection '{}' loaded into memory", collectionName);
        } catch (Exception e) {
            log.error("Failed to load collection", e);
        }
    }

    public void insert(String content, float[] vector, String sourceType, String sourceId,
                       int verified, int likeCount) {
        try {
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("content", List.of(content)));
            fields.add(new InsertParam.Field("vector", List.of(toFloatList(vector))));
            fields.add(new InsertParam.Field("source_type", List.of(sourceType)));
            fields.add(new InsertParam.Field("source_id", List.of(sourceId)));
            fields.add(new InsertParam.Field("verified", List.of((long) verified)));
            fields.add(new InsertParam.Field("like_count", List.of((long) likeCount)));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            R<io.milvus.grpc.MutationResult> insertResult = client.insert(insertParam);
            log.info("Inserted into Milvus: sourceType={}, sourceId={}, verified={}, result={}",
                    sourceType, sourceId, verified, insertResult.getStatus());
        } catch (Exception e) {
            log.error("Failed to insert into Milvus", e);
        }
    }

    public List<Map<String, Object>> search(float[] queryVector, int topK) {
        try {
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withVectors(List.of(toFloatList(queryVector)))
                    .withVectorFieldName("vector")
                    .withTopK(topK)
                    .withMetricType(MetricType.COSINE)
                    .withOutFields(List.of("content", "source_type", "source_id", "verified", "like_count"))
                    .withConsistencyLevel(ConsistencyLevelEnum.BOUNDED)
                    .build();

            R<SearchResults> searchResult = client.search(searchParam);
            SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResult.getData().getResults());

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (SearchResultsWrapper.IDScore idScore : wrapper.getIDScore(0)) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", idScore.getLongID());
                item.put("score", idScore.getScore());
                item.put("content", idScore.get("content"));
                item.put("source_type", idScore.get("source_type"));
                item.put("source_id", idScore.get("source_id"));
                item.put("verified", idScore.get("verified"));
                item.put("like_count", idScore.get("like_count"));
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
