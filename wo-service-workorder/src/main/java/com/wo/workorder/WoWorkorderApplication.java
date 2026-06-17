package com.wo.workorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {ElasticsearchRepositoriesAutoConfiguration.class})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wo.api.client")
@MapperScan("com.wo.workorder.mapper")
@ComponentScan(basePackages = {"com.wo.workorder", "com.wo.common"})
public class WoWorkorderApplication {

    public static void main(String[] args) {
        SpringApplication.run(WoWorkorderApplication.class, args);
    }
}
