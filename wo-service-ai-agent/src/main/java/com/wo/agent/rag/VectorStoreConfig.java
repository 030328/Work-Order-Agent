package com.wo.agent.rag;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${milvus.host:127.0.0.1}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    /**
     * 创建 Milvus 客户端 Bean
     *
     * @return MilvusClient
     */
    @Bean
    public MilvusClient milvusClient() {
        log.info("Connecting to Milvus at {}:{}", host, port);

        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();

        MilvusServiceClient client = new MilvusServiceClient(connectParam);
        log.info("Milvus client connected successfully");

        return client;
    }
}
