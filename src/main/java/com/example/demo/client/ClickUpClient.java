package com.example.demo.client;

import com.example.demo.dto.ClickUpSpaceListResponseDto;
import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.dto.ClickUpFolderListResponseDto;
import com.example.demo.dto.ClickUpListResponseDto;
import com.example.demo.dto.ClickUpTaskResponseDto;
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
            @Value("${clickup.api.token}") String token,
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

    //folder
    public ClickUpFolderListResponseDto getFolders(String spaceId) {
        return restClient.get()
                .uri(folderUri, spaceId)
                .retrieve()
                .body(ClickUpFolderListResponseDto.class);
    }

    //spacedeki list
    public ClickUpListResponseDto getListsBySpace(String spaceId) {
        return restClient.get()
                .uri(spaceListUri, spaceId)
                .retrieve()
                .body(ClickUpListResponseDto.class);
    }

    //TASKS
    public ClickUpTaskResponseDto getTasks(String teamId) {
        return restClient.get()
                .uri(taskUri + "?include_closed=true", teamId)
                .retrieve()
                .body(ClickUpTaskResponseDto.class);
    }

}