package com.wo.workorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wo.api.client")
@MapperScan("com.wo.workorder.mapper")
public class WoWorkorderApplication {

    public static void main(String[] args) {
        SpringApplication.run(WoWorkorderApplication.class, args);
    }
}
