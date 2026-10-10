package com.example.demo.client;

import com.example.demo.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClickUpClient {

    private final RestClient restClient;
    private final String teamUri;
    private final String spaceUri;
    private final String folderUri;
    private final String spaceListUri;
    private final String taskUri;

    public ClickUpClient(
            @Value("${clickup.api.base-url}") String baseUrl,
            @Value("${clickup.api.endpoints.teams}") String teamUri,
            @Value("${clickup.api.endpoints.spaces}") String spaceUri,
            @Value("${clickup.api.endpoints.folders}") String folderUri,
            @Value("${clickup.api.endpoints.space_list}") String spaceListUri,
            @Value("${clickup.api.endpoints.tasks}") String taskUri
    ) {
        this.teamUri = teamUri;
        this.spaceUri = spaceUri;
        this.folderUri = folderUri;
        this.spaceListUri = spaceListUri;
        this.taskUri = taskUri;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public String exchangeCodeForToken(String clientId, String clientSecret, String code) {
        ClickUpOAuthTokenResponseDto response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/oauth/token")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("code", code)
                        .build())
                .retrieve()
                .body(ClickUpOAuthTokenResponseDto.class);

        return response != null ? response.accessToken() : null;
    }

    public ClickUpAuthorizedUserResponseDto getAuthorizedUser(String accessToken) {
        return restClient.get()
                .uri("/user")
                .header("Authorization", formatToken(accessToken))
                .retrieve()
                .body(ClickUpAuthorizedUserResponseDto.class);
    }

    public ClickUpTeamResponseDto getTeamsByToken(String accessToken) {
        return getTeams(accessToken);
    }

    public ClickUpTeamResponseDto getTeams(String accessToken) {
        return restClient.get()
                .uri(teamUri)
                .header("Authorization", formatToken(accessToken))
                .retrieve()
                .body(ClickUpTeamResponseDto.class);
    }

    public ClickUpSpaceListResponseDto getSpaces(String teamId, String accessToken) {
        return restClient.get()
                .uri(spaceUri, teamId)
                .header("Authorization", formatToken(accessToken))
                .retrieve()
                .body(ClickUpSpaceListResponseDto.class);
    }

    public ClickUpFolderListResponseDto getFolders(String spaceId, String accessToken) {
        return restClient.get()
                .uri(folderUri, spaceId)
                .header("Authorization", formatToken(accessToken))
                .retrieve()
                .body(ClickUpFolderListResponseDto.class);
    }

    public ClickUpListResponseDto getListsBySpace(String spaceId, String accessToken) {
        return restClient.get()
                .uri(spaceListUri, spaceId)
                .header("Authorization", formatToken(accessToken))
                .retrieve()
                .body(ClickUpListResponseDto.class);
    }

    public ClickUpTaskResponseDto getTasks(String teamId, String accessToken) {
        return restClient.get()
                .uri(taskUri + "?include_closed=true", teamId)
                .header("Authorization", formatToken(accessToken))
                .retrieve()
                .body(ClickUpTaskResponseDto.class);
    }

    private String formatToken(String token) {
        if (token == null) return "";
        if (token.startsWith("pk_") || token.startsWith("Bearer ")) {
            return token;
        }
        return "Bearer " + token;
    }
}