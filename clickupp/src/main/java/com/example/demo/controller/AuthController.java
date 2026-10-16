package com.example.demo.controller;

import com.example.demo.entity.UserEntity;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestParam(value = "error", required = false) String oauthError
    ) {
        if (oauthError != null) {
            log.warn("ClickUp OAuth authorization was denied or failed");
            return ResponseEntity.badRequest().body("ClickUp authorization was not granted.");
        }
        if (code == null || code.isBlank()) {
            log.warn("ClickUp OAuth callback received without an authorization code");
            return ResponseEntity.badRequest().body("ClickUp did not return an authorization code.");
        }

        log.info("ClickUp OAuth callback received; exchanging authorization code");
        UserEntity user;
        try {
            user = authService.loginWithClickUpCode(code);
        } catch (RestClientResponseException exception) {
            log.warn("ClickUp OAuth callback failed; upstream returned HTTP {}",
                    exception.getStatusCode().value());
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