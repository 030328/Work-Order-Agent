package com.wo.agent.rag;

import io.milvus.client.MilvusClient;
import io.milvus.common.clientdef.CollectionInfo;
import io.milvus.grpc.DataType;
import io.milvus.grpc.FieldSchema;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.*;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Milvus 向量数据库客户端封装
 * 提供集合管理、向量插入、相似度搜索等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusClientWrapper {

    private final MilvusClient milvusClient;

    /**
     * 创建向量集合
     *
     * @param collectionName 集合名称
     * @param dimension      向量维度
     */
    public void createCollection(String collectionName, int dimension) {
        try {
            // 检查集合是否已存在
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());

            if (hasCollection.getData()) {
                log.info("Collection {} already exists", collectionName);
                return;
            }

            // 定义字段
            List<FieldSchema> fields = new ArrayList<>();

            // 主键字段
            fields.add(FieldSchema.newBuilder()
                    .setName("id")
                    .setDataType(DataType.VarChar)
                    .setMaxLength(64)
                    .setIsPrimaryKey(true)
                    .build());

            // 向量字段
            fields.add(FieldSchema.newBuilder()
                    .setName("vector")
                    .setDataType(DataType.FloatVector)
                    .setDataTypeParams(Map.of("dim", String.valueOf(dimension)))
                    .build());

            // 内容字段
            fields.add(FieldSchema.newBuilder()
                    .setName("content")
                    .setDataType(DataType.VarChar)
                    .setMaxLength(65535)
                    .build());

            // 元数据字段
            fields.add(FieldSchema.newBuilder()
                    .setName("source_id")
                    .setDataType(DataType.VarChar)
                    .setMaxLength(128)
                    .build());

            fields.add(FieldSchema.newBuilder()
                    .setName("source_type")
                    .setDataType(DataType.VarChar)
                    .setMaxLength(64)
                    .build());

            fields.add(FieldSchema.newBuilder()
                    .setName("title")
                    .setDataType(DataType.VarChar)
                    .setMaxLength(512)
                    .build());

            // 创建集合
            CreateCollectionParam param = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withSchema(fields)
                    .build();

            milvusClient.createCollection(param);
            log.info("Created collection: {} with dimension: {}", collectionName, dimension);

            // 创建向量索引
            milvusClient.createIndex(
                    io.milvus.param.index.CreateIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withFieldName("vector")
                            .withIndexType(io.milvus.grpc.IndexType.HNSW)
                            .withMetricType(io.milvus.grpc.MetricType.L2)
                            .withExtraParam("{\"M\":16,\"efConstruction\":256}")
                            .build());

            log.info("Created HNSW index on collection: {}", collectionName);

        } catch (Exception e) {
            log.error("Failed to create collection: {}", collectionName, e);
            throw new RuntimeException("创建向量集合失败: " + collectionName, e);
        }
    }

    /**
     * 插入向量和元数据
     *
     * @param collectionName 集合名称
     * @param id             记录ID
     * @param vector         向量数组
     * @param content        文本内容
     * @param metadata       元数据
     */
    public void insert(String collectionName, String id, float[] vector, String content,
                       Map<String, String> metadata) {
        try {
            List<JsonObject> data = new ArrayList<>();
            JsonObject row = new JsonObject();
            row.addProperty("id", id);
            row.add("vector", gson.toJsonTree(vector));
            row.addProperty("content", content);
            row.addProperty("source_id", metadata.getOrDefault("sourceId", ""));
            row.addProperty("source_type", metadata.getOrDefault("sourceType", ""));
            row.addProperty("title", metadata.getOrDefault("title", ""));
            data.add(row);

            R<io.milvus.grpc.MutationResult> result = milvusClient.insert(
                    InsertParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withRows(data)
                            .build());

            if (result.getStatus() == R.Status.Success.getCode()) {
                log.debug("Inserted vector: {} into collection: {}", id, collectionName);
            } else {
                log.error("Failed to insert vector: {}, status: {}", id, result.getStatus());
            }

        } catch (Exception e) {
            log.error("Failed to insert vector: {} into collection: {}", id, collectionName, e);
            throw new RuntimeException("插入向量失败", e);
        }
    }

    /**
     * 向量相似度搜索
     *
     * @param collectionName 集合名称
     * @param queryVector    查询向量
     * @param topK           返回结果数量
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String collectionName, float[] queryVector, int topK) {
        try {
            // 加载集合到内存
            milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());

            // 执行搜索
            R<SearchResults> response = milvusClient.search(
                    SearchParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withVectors(List.of(queryVector))
                            .withVectorFieldName("vector")
                            .withTopK(topK)
                            .withMetricType(io.milvus.grpc.MetricType.L2)
                            .withOutFields(List.of("id", "content", "source_id", "source_type", "title"))
                            .build());

            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("Search failed, status: {}", response.getStatus());
                return Collections.emptyList();
            }

            // 解析结果
            SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);

            List<SearchResult> results = new ArrayList<>();
            for (SearchResultsWrapper.IDScore score : scores) {
                SearchResult result = new SearchResult();
                result.setId(score.get("id").toString());
                result.setContent(score.get("content").toString());
                result.setScore(1.0f - score.getScore()); // L2 距离转换为相似度

                Map<String, String> metadata = new HashMap<>();
                metadata.put("sourceId", score.get("source_id").toString());
                metadata.put("sourceType", score.get("source_type").toString());
                metadata.put("title", score.get("title").toString());
                result.setMetadata(metadata);

                results.add(result);
            }

            log.debug("Search returned {} results from collection: {}", results.size(), collectionName);
            return results;

        } catch (Exception e) {
            log.error("Search failed in collection: {}", collectionName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 按来源ID删除向量
     *
     * @param collectionName 集合名称
     * @param sourceId       来源ID
     */
    public void deleteBySourceId(String collectionName, String sourceId) {
        try {
            R<io.milvus.grpc.MutationResult> result = milvusClient.delete(
                    DeleteParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withExpr("source_id == \"" + sourceId + "\"")
                            .build());

            if (result.getStatus() == R.Status.Success.getCode()) {
                log.info("Deleted vectors by sourceId: {} from collection: {}", sourceId, collectionName);
            } else {
                log.error("Failed to delete vectors by sourceId: {}, status: {}", sourceId, result.getStatus());
            }

        } catch (Exception e) {
            log.error("Failed to delete vectors by sourceId: {} from collection: {}", sourceId, collectionName, e);
            throw new RuntimeException("删除向量失败", e);
        }
    }

    /**
     * 向量搜索结果
     */
    @Data
    public static class SearchResult {
        private String id;
        private String content;
        private float score;
        private Map<String, String> metadata;
    }

    // JSON 工具（实际项目中应注入或使用 Jackson）
    private static final com.google.gson.Gson gson = new com.google.gson.Gson();
}
