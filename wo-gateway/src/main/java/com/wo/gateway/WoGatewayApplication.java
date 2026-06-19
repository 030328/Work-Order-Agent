package com.wo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        RedisAutoConfiguration.class
}, scanBasePackages = "com.wo.gateway")
@EnableDiscoveryClient
public class WoGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(WoGatewayApplication.class, args);
    }
}
