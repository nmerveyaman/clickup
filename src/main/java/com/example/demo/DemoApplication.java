package com.example.demo;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.example.demo.service.ClickUpSyncService;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(DemoApplication.class, args);
	}

	private static void loadDotEnv() {
		Dotenv.configure()
				.ignoreIfMissing()
				.load()
				.entries()
				.forEach(e -> {
					if (System.getProperty(e.getKey()) == null && System.getenv(e.getKey()) == null) {
						System.setProperty(e.getKey(), e.getValue());
					}
				});
	}

	@Bean
	CommandLineRunner run(ClickUpSyncService syncService) {
		return args -> {
			System.out.println(">>> ClickUp Senkronizasyonu tetikleniyor...");
			syncService.syncClickUpData();
		};
	}
}
