package com.threelinelimited.rentalservice.service;

import com.threelinelimited.rentalservice.dto.Asset;
import com.threelinelimited.rentalservice.model.AssetEntity;
import com.threelinelimited.rentalservice.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetDaoService {
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AssetRepository assetRepository;

    public AssetEntity save(AssetEntity entity) {
        return assetRepository.save(entity);
    }

    public Optional<AssetEntity> findByCode(String code) {
        return assetRepository.findByCode(code);
    }

    public Optional<AssetEntity> findByCodeForUpdate(String code) {
        return assetRepository.findByCodeForUpdate(code);
    }

    public Page<AssetEntity> findByMerchant(String username, Pageable pageable) {
        return assetRepository.findByMerchantUsernameOrderByIdDesc(username, pageable);
    }

    public String generateAssetCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("AST-");
            for (int i = 0; i < 8; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            code = sb.toString();
        } while (assetRepository.existsByCode(code));
        return code;
    }

    public static Asset toDto(AssetEntity e) {
        return Asset.builder()
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .category(e.getCategory())
                .dailyRate(e.getDailyRate())
                .currency(e.getCurrency())
                .quantity(e.getQuantity())
                .location(e.getLocation())
                .status(e.getStatus())
                .merchantCode(e.getMerchant().getCode())
                .createdOn(e.getCreatedOn())
                .build();
    }
}
