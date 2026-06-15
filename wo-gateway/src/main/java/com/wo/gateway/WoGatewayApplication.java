package com.wo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class WoGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(WoGatewayApplication.class, args);
    }
}
