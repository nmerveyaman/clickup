package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "http://localhost").replaceAll("/+$", "");
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost",
                        "http://localhost:80",
                        "http://localhost:5173",
                        "http://127.0.0.1"
                )
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", frontendUrl)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type", "Cache-Control")
                .allowCredentials(true)
                .maxAge(3600);
    }
}