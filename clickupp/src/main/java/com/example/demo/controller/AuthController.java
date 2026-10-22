package com.example.demo.controller;

import com.example.demo.entity.UserEntity;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        String clickUpLoginUrl = authService.getClickUpLoginUrl();
        String callbackUri = UriComponentsBuilder.fromUriString(clickUpLoginUrl)
                .build()
                .getQueryParams()
                .getFirst("redirect_uri");
        log.info("Starting ClickUp OAuth; configured callback URI={}", callbackUri);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(clickUpLoginUrl))
                .build();
    }

    @GetMapping({"/callback", "/"})
    public ResponseEntity<?> handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String oauthError,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request
    ) {
        if (oauthError != null) {
            log.warn("ClickUp OAuth authorization was denied or failed");
            return ResponseEntity.badRequest().body("ClickUp authorization was not granted.");
        }
        if (code == null || code.isBlank()) {
            log.warn("ClickUp OAuth callback received without an authorization code");
            return ResponseEntity.badRequest().body("ClickUp did not return an authorization code.");
        }

        // 1. IP Adresini Güvenli Şekilde Yakala (Render/Cloudflare uyumlu)
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }

        log.info("ClickUp OAuth callback received from IP={} | User-Agent={}; exchanging code", clientIp, userAgent);

        UserEntity user;
        try {
            // 2. AuthService'e 3 parametreyi de eksiksiz gönderiyoruz
            user = authService.loginWithClickUpCode(code, userAgent, clientIp);
        } catch (RestClientResponseException exception) {
            log.warn("ClickUp OAuth callback failed for IP={}; upstream returned HTTP {}",
                    clientIp, exception.getStatusCode().value());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("ClickUp authentication failed (upstream HTTP "
                            + exception.getStatusCode().value() + ").");
        }

        String defaultWorkspaceId = authService.getUserDefaultWorkspaceId(user.getId());

        String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "http://localhost")
                .replaceAll("/+$", "");

        URI targetUri = UriComponentsBuilder.fromHttpUrl(frontendUrl + "/dashboard")
                .queryParam("userId", user.getId())
                .queryParam("username", user.getUsername())
                .queryParam("role", user.getAppRole())
                .queryParam("workspaceId", defaultWorkspaceId)
                .encode()
                .build()
                .toUri();

        log.info("ClickUp OAuth completed for userId={}; workspaceIdPresent={}",
                user.getId(), !defaultWorkspaceId.isBlank());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(targetUri)
                .build();
    }
}