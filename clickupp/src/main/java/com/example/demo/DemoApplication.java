package com.example.demo;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.example.demo.service.ClickUpSyncService;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(DemoApplication.class, args);
	}

	private static void loadDotEnv() {
		Path workingDirectory = Path.of("").toAbsolutePath().normalize();
		for (Path directory : new Path[]{workingDirectory, workingDirectory.getParent()}) {
			if (directory == null || !Files.isRegularFile(directory.resolve(".env"))) {
				continue;
			}

			Dotenv.configure()
					.directory(directory.toString())
					.ignoreIfMissing()
					.load()
					.entries()
					.forEach(entry -> {
						String systemProperty = System.getProperty(entry.getKey());
						String environmentVariable = System.getenv(entry.getKey());
						if ((systemProperty == null || systemProperty.isBlank())
								&& (environmentVariable == null || environmentVariable.isBlank())) {
							System.setProperty(entry.getKey(), entry.getValue());
						}
					});
		}
	}

	@Bean
	CommandLineRunner run(ClickUpSyncService syncService) {
		return args -> {
			System.out.println(">>> ClickUp Senkronizasyonu tetikleniyor...");
			syncService.syncAllConnectedWorkspaces();
		};
	}
}
