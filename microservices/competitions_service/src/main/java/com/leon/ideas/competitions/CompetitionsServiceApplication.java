package com.leon.ideas.competitions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CompetitionsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompetitionsServiceApplication.class, args);
	}

}




