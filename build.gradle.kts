plugins {
	java
	id("org.springframework.boot") version "4.2.0-M1"
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

val mapstructVersion = "1.5.5.Final"
val testcontainersVersion = "1.20.4"

dependencies {
	// REST API
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	// JPA
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// PostgreSQL
	runtimeOnly("org.postgresql:postgresql")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// MapStruct
	implementation("org.mapstruct:mapstruct:$mapstructVersion")
	annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

	// Lombok & MapStruct Binding
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Testcontainers (Versiyonlar eklendi)
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
	testImplementation("org.testcontainers:postgresql:$testcontainersVersion")

	//Swagger
	//testImplementation("org.springdoc.springdoc-openapi-starter-webmvc-ui:2.3.0")

	// .env dosyası yükleme
	implementation("io.github.cdimascio:dotenv-java:3.0.0")

	// Gson
	implementation("com.google.code.gson:gson")

	// Flyway
	//implementation("org.flywaydb:flyway-core:13.8.0")
	//implementation("org.flywaydb:flyway-database-postgresql:13.8.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}