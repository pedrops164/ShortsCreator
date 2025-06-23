package com.content_storage_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;

@SpringBootApplication
@EnableReactiveMethodSecurity
public class ContentStorageServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContentStorageServiceApplication.class, args);
	}

}
