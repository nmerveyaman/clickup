package com.example.demo.service;

import com.example.demo.client.ClickUpClient;
import com.example.demo.dto.ClickUpAuthorizedUserResponseDto;
import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.entity.UserEntity;
import com.example.demo.enums.Role;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClickUpClient clickUpClient;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ClickUpSyncService clickUpSyncService;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Value("${clickup.oauth.client-id}")
    private String clientId;

    @Value("${clickup.oauth.client-secret}")
    private String clientSecret;

    @Value("${clickup.oauth.redirect-uri}")
    private String redirectUri;

    public String getClickUpLoginUrl() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("CLICKUP_CLIENT_ID yapılandırılmamış.");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException("CLICKUP_REDIRECT_URI yapılandırılmamış.");
        }

        return UriComponentsBuilder.fromUriString("https://app.clickup.com/api")
                .queryParam("client_id", clientId.trim())
                .queryParam("redirect_uri", redirectUri.trim())
                .build()
                .encode()
                .toUriString();
    }

    @Transactional
    public UserEntity loginWithClickUpCode(String code, String userAgent, String clientIp) {
        // 1. ClickUp'tan kullanıcının kişisel access_token'ını al
        System.out.println("ClickUp Giriş İsteği -> IP: " + clientIp + ", User-Agent: " + userAgent);
        String accessToken = clickUpClient.exchangeCodeForToken(clientId, clientSecret, code);
        // 2. Bu token kime ait? ClickUp'tan profilini çek
        ClickUpAuthorizedUserResponseDto authUser = clickUpClient.getAuthorizedUser(accessToken);
        var userDto = authUser.user();

        // 3. Kullanıcı DB'de varsa güncelle, yoksa yeni oluştur (UPSERT)
        UserEntity user = userRepository.findById(userDto.id())
                .map(existing -> {
                    existing.setUsername(userDto.username());
                    existing.setEmail(userDto.email());
                    existing.setProfilePicture(userDto.profilePicture());
                    return existing;
                })
                .orElseGet(() -> userMapper.toEntity(userDto));

        // 4. Kişisel OAuth token'ını kaydet/güncelle
        user.setAccessToken(accessToken);

        // 5. Rolü henüz atanmamışsa ClickUp'taki role_key'e göre otomatik ata
        if (user.getAppRole() == null) {
            user.setAppRole(determineInitialRole(user.getId(), accessToken));
        }

        // 6. Kullanıcıyı DB'ye kaydet
        UserEntity savedUser = userRepository.save(user);

        // 7. Kullanıcı kaydedilir kaydedilmez, onun token'ı ile Workspace verilerini hemen çek!
        clickUpSyncService.syncForSingleUser(accessToken);

        return savedUser;
    }

    /**
     * Kullanıcının üyesi olduğu ilk senkronize Workspace ID'sini getirir.
     */
    @Transactional(readOnly = true)
    public String getUserDefaultWorkspaceId(Long userId) {
        return workspaceMemberRepository.findFirstByUserIdOrderByIdAsc(userId)
                .map(member -> member.getWorkspaceId())
                .orElse("");
    }

    private Role determineInitialRole(Long userId, String accessToken) {
        ClickUpTeamResponseDto teams = clickUpClient.getTeamsByToken(accessToken);
        if (teams != null && teams.workspaces() != null) {
            for (var ws : teams.workspaces()) {
                if (ws.members() != null) {
                    for (var member : ws.members()) {
                        if (member.user() != null && userId.equals(member.user().id())) {
                            String roleKey = member.user().roleKey();
                            if (roleKey != null && (roleKey.equalsIgnoreCase("owner") || roleKey.equalsIgnoreCase("admin"))) {
                                return Role.TEAM_LEAD;
                            }
                        }
                    }
                }
            }
        }
        return Role.DEVELOPER;
    }
}