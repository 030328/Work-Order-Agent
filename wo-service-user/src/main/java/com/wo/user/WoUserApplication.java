package com.wo.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wo.api.client")
@MapperScan("com.wo.user.mapper")
public class WoUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(WoUserApplication.class, args);
    }
}
