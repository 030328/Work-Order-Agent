package com.wo.workflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wo.api.client")
@MapperScan("com.wo.workflow.mapper")
@ComponentScan(basePackages = {"com.wo.workflow", "com.wo.common"})
@EnableScheduling
public class WoWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WoWorkflowApplication.class, args);
    }
}
