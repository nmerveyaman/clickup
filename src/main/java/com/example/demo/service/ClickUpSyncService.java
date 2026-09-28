package com.example.demo.service;

import com.example.demo.client.ClickUpClient;
import com.example.demo.dto.ClickUpSpaceListResponseDto;
import com.example.demo.dto.ClickUpSpaceListResponseDto.ClickUpSpaceDto;
import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.entity.SpaceEntity;
import com.example.demo.repository.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickUpSyncService {

    private final ClickUpClient clickUpClient;
    private final SpaceRepository spaceRepository;

    public ClickUpSyncService(ClickUpClient clickUpClient, SpaceRepository spaceRepository) {
        this.clickUpClient = clickUpClient;
        this.spaceRepository = spaceRepository;
    }

    @Transactional
    public void syncSpacesFromClickUp() {
        // 1. Önce Team (Workspace) ID'sini dinamik olarak çekiyoruz
        ClickUpTeamResponseDto teamResponse = clickUpClient.getTeams();
        if (teamResponse == null || teamResponse.teams() == null || teamResponse.teams().isEmpty()) {
            System.out.println(">>> HATA: Hiçbir Team (Workspace) bulunamadı! Token'ınızı kontrol edin.");
            return;
        }

        // Listeden ilk ekibin ID'sini alıyoruz
        String teamId = teamResponse.teams().get(0).id();
        System.out.println(">>> Bulunan Team ID: " + teamId);

        // 2. Bu Team ID'yi kullanarak Space'leri çekiyoruz
        ClickUpSpaceListResponseDto spaceResponse = clickUpClient.getSpaces(teamId);
        if (spaceResponse == null || spaceResponse.spaces() == null) {
            System.out.println(">>> Çekilecek Space bulunamadı.");
            return;
        }

        // 3. Gelen DTO nesnelerini Entity'ye dönüştürüp veritabanına kaydediyoruz
        for (ClickUpSpaceDto dto : spaceResponse.spaces()) {
            SpaceEntity entity = new SpaceEntity();
            entity.setId(dto.id());
            entity.setName(dto.name());
            entity.setColor(dto.color());
            entity.setIsPrivate(dto.isPrivate());
            entity.setIsArchived(dto.isArchived());

            // PostgreSQL'e kaydet (Eğer ID veritabanında varsa günceller, yoksa yeni ekler)
            spaceRepository.save(entity);
            System.out.println(">>> Veritabanına kaydedildi -> Space: " + dto.name());
        }

        System.out.println(">>> Tüm Space senkronizasyonu başarıyla tamamlandı!");
    }
}