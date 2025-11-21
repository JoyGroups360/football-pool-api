package com.leon.ideas.groups;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GroupsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GroupsServiceApplication.class, args);
    }
}