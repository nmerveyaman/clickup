package com.example.demo.service;

import com.example.demo.client.ClickUpClient;
import com.example.demo.dto.ClickUpAuthorizedUserResponseDto;
import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.entity.UserEntity;
import com.example.demo.enums.Role;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClickUpClient clickUpClient;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ClickUpSyncService clickUpSyncService; // <-- 1. EKLENEN: Senkronizasyon servisini buraya bağladık

    @Value("${clickup.oauth.client-id}")
    private String clientId;

    @Value("${clickup.oauth.client-secret}")
    private String clientSecret;

    @Value("${clickup.oauth.redirect-uri}")
    private String redirectUri;

    public String getClickUpLoginUrl() {
        return "https://app.clickup.com/api?client_id=" + clientId + "&redirect_uri=" + redirectUri;
    }

    @Transactional
    public UserEntity loginWithClickUpCode(String code) {
        // 1. ClickUp'tan kullanıcının kişisel access_token'ını al
        String accessToken = clickUpClient.exchangeCodeForToken(clientId, clientSecret, code);

        // 2. Bu token kime ait? ClickUp'tan profilini çek
        ClickUpAuthorizedUserResponseDto authUser = clickUpClient.getAuthorizedUser(accessToken);
        var userDto = authUser.user();

        // 3. Kullanıcı DB'de varsa güncelle, yoksa yeni oluştur
        UserEntity user = userRepository.findById(userDto.id())
                .map(existing -> {
                    existing.setUsername(userDto.username());
                    existing.setEmail(userDto.email());
                    existing.setProfilePicture(userDto.profilePicture());
                    return existing;
                })
                .orElseGet(() -> userMapper.toEntity(userDto));

        // 4. Kişisel OAuth token'ını kaydet
        user.setAccessToken(accessToken);

        // 5. Rolü henüz atanmamışsa ClickUp'taki role_key'e göre otomatik ata
        if (user.getAppRole() == null) {
            user.setAppRole(determineInitialRole(user.getId(), accessToken));
        }

        // 6. Kullanıcıyı DB'ye kaydet
        UserEntity savedUser = userRepository.save(user);

        // <-- 2. EKLENEN: Kullanıcı kaydedilir kaydedilmez, onun token'ı ile Workspace verilerini hemen çek!
        clickUpSyncService.syncForSingleUser(accessToken);

        return savedUser;
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