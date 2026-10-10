# 1. Aşama: Build (Gradle ile JAR oluşturma)
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

# Gradle wrapper ve bağımlılık dosyalarını önbelleğe almak için kopyala
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./

# Çalıştırma izni ver ve bağımlılıkları indir
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Kaynak kodları kopyala ve executable JAR'ı oluştur (Testleri atlayarak)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 2. Aşama: Runtime (Uygulamayı Çalıştırma)
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Oluşturulan JAR dosyasını kopyala
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]