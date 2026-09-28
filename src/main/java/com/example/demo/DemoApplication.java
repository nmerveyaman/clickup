package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.example.demo.service.ClickUpSyncService;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	CommandLineRunner run(ClickUpSyncService syncService) {
		return args -> {
			System.out.println(">>> ClickUp Senkronizasyonu tetikleniyor...");
			syncService.syncSpacesFromClickUp();
		};
	}
}