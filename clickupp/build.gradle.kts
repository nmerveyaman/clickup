plugins {
	java
	idea
	id("org.springframework.boot") version "3.5.16"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

val mapstructVersion = "1.6.3"
val lombokVersion = "1.18.42"
val testcontainersVersion = "1.20.4"

dependencies {
	// REST API
	implementation("org.springframework.boot:spring-boot-starter-web")
	// JPA
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework:spring-tx")
	//flyway
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	// PostgreSQL
	runtimeOnly("org.postgresql:postgresql")

	// Lombok (Java 25 uyumlu sürüm zorunlu kılındı)
	compileOnly("org.projectlombok:lombok:$lombokVersion")
	annotationProcessor("org.projectlombok:lombok:$lombokVersion")

	// MapStruct
	implementation("org.mapstruct:mapstruct:$mapstructVersion")
	annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

	// Lombok & MapStruct Binding
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Testcontainers
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
	testImplementation("org.testcontainers:postgresql:$testcontainersVersion")

	// Swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

	// .env dosyası yükleme
	implementation("io.github.cdimascio:dotenv-java:3.0.0")

	// h'bernate validator dto validasyonları için
	implementation("org.springframework.boot:spring-boot-starter-validation")
	// Gson
	implementation("com.google.code.gson:gson")
}

tasks.withType<Test> {
	useJUnitPlatform()
}