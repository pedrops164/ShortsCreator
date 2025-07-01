package com.content_storage_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

@SpringBootApplication
@EnableReactiveMongoAuditing
public class ContentStorageServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContentStorageServiceApplication.class, args);
	}

}
