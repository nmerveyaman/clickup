package com.example.demo.controller;

import com.example.demo.entity.UserEntity;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        String clickUpLoginUrl = authService.getClickUpLoginUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(clickUpLoginUrl))
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam("code") String code) {
        // 1. OAuth kodunu exchange et ve kullanıcıyı kaydet/güncelle
        UserEntity user = authService.loginWithClickUpCode(code);

        // 2. Kullanıcının varsayılan workspace id'sini al
        String defaultWorkspaceId = authService.getUserDefaultWorkspaceId(user.getId());

        // 3. Frontend adresini dinamik oku (Varsayılan: Nginx portu olan http://localhost)
        String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "http://localhost");

        // 4. UriComponentsBuilder ile encode() çağırarak URI nesnesine çeviriyoruz
        URI targetUri = UriComponentsBuilder.fromHttpUrl(frontendUrl + "/dashboard")
                .queryParam("userId", user.getId())
                .queryParam("username", user.getUsername())
                .queryParam("role", user.getAppRole())
                .queryParam("workspaceId", defaultWorkspaceId)
                .encode() // <-- KRİTİK NOKTA: Boşlukları %20 formatına encode eder
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(targetUri)
                .build();
    }
}