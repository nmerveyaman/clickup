package com.example.demo.client;

import com.example.demo.dto.ClickUpSpaceListResponseDto;
import com.example.demo.dto.ClickUpTeamResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClickUpClient {

    private final RestClient restClient;
    @Value("${clickup.api.endpoints.teams}")
    private String teamUri;
    @Value("${clickup.api.endpoints.spaces}")
    private String spaceUri;

    public ClickUpClient(
            @Value("${clickup.api.base-url}") String baseUrl,
            @Value("${clickup.api.token}") String token
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", token)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public ClickUpTeamResponseDto getTeams() {
        return restClient.get()
                .uri(teamUri)
                .retrieve()
                .body(ClickUpTeamResponseDto.class);
    }

    public ClickUpSpaceListResponseDto getSpaces(String teamId) {
        return restClient.get()
                .uri(spaceUri, teamId)
                .retrieve()
                .body(ClickUpSpaceListResponseDto.class);
    }
}