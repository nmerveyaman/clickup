package com.example.demo.controller;

import com.example.demo.entity.UserEntity;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. Giriş Başlatma: http://localhost:8080/login
    @GetMapping({"/login", "/api/auth/clickup/login"})
    public ResponseEntity<Void> redirectToClickUp() {
        String url = authService.getClickUpLoginUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    // 2. ClickUp'tan Dönüş: Kodu kullanır ve anında /auth/success sayfasına yönlendirir!
    @GetMapping(value = {"/", "/api/auth/clickup/callback"}, params = "code")
    public ResponseEntity<Void> handleCallback(@RequestParam("code") String code) {
        System.out.println("ClickUp'tan gelen code yakalandı: " + code);
        UserEntity user = authService.loginWithClickUpCode(code);

        String successUrl = "http://localhost:5173/?userId=" + user.getId()
                + "&username=" + user.getUsername()
                + "&role=" + user.getAppRole();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(successUrl))
                .build();
    }

    // 3. Temiz Başarılı Ekranı (Sayfayı yenilesen de hata vermez!)
    @GetMapping("/auth/success")
    public ResponseEntity<Map<String, Object>> authSuccess(
            @RequestParam Long userId,
            @RequestParam String username,
            @RequestParam String role
    ) {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "ClickUp OAuth girişi ve senkronizasyon başarılı!",
                "userId", userId,
                "username", username,
                "role", role
        ));
    }
}