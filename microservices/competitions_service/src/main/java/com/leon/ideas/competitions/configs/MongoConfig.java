package com.leon.ideas.competitions.configs;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;


@Configuration
public class MongoConfig {

    @Bean
    @Primary
    public MongoTemplate competitionsMongoTemplate() {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017/football-pool-dev");
        return new MongoTemplate(mongoClient, "football-pool-dev");
    }
}

