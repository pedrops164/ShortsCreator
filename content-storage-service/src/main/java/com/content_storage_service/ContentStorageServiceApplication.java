package com.content_storage_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;

@SpringBootApplication
@EnableReactiveMethodSecurity
@EnableReactiveMongoAuditing
public class ContentStorageServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContentStorageServiceApplication.class, args);
	}

}
