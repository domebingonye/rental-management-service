package com.threelinelimited.rentalservice.service;

import com.threelinelimited.rentalservice.config.PropsReader;
import com.threelinelimited.rentalservice.dto.Asset;
import com.threelinelimited.rentalservice.dto.request.CreateAssetRequest;
import com.threelinelimited.rentalservice.enums.AccountStatus;
import com.threelinelimited.rentalservice.enums.AssetStatus;
import com.threelinelimited.rentalservice.enums.ResponseCodes;
import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.exception.RequestException;
import com.threelinelimited.rentalservice.model.AssetEntity;
import com.threelinelimited.rentalservice.model.MerchantProfileEntity;
import com.threelinelimited.rentalservice.model.SystemUserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssetService {
    private final AssetDaoService assetDaoService;
    private final SystemUserDaoService systemUserDaoService;
    private final MerchantProfileDaoService merchantProfileDaoService;
    private final PropsReader propsReader;

    @Transactional
    public Asset createAsset(String merchantUsername, CreateAssetRequest request) {
        SystemUserEntity merchant = systemUserDaoService.findByUsername(merchantUsername)
                .filter(u -> u.getUserRoleType() == UserRoleType.MERCHANT)
                .orElseThrow(() -> new RequestException(ResponseCodes.FORBIDDEN, "Only merchants can create assets"));
        if (merchant.getStatus() != AccountStatus.ACTIVE) {
            throw new RequestException(ResponseCodes.FORBIDDEN, "Merchant account is not active");
        }

        AssetEntity asset = assetDaoService.save(new AssetEntity()
                .setCode(assetDaoService.generateAssetCode())
                .setMerchant(merchant)
                .setName(request.getName().trim())
                .setDescription(trimToNull(request.getDescription()))
                .setCategory(request.getCategory())
                .setDailyRate(request.getDailyRate().setScale(2, RoundingMode.UNNECESSARY))
                .setCurrency(propsReader.getWalletCurrency())
                .setQuantity(request.getQuantity())
                .setLocation(trimToNull(request.getLocation()))
                .setStatus(AssetStatus.AVAILABLE));

        log.info("Merchant {} created asset {} ({})", merchant.getCode(), asset.getCode(), asset.getName());
        return withMerchantName(asset);
    }

    @Transactional(readOnly = true)
    public Asset getAsset(String code) {
        return assetDaoService.findByCode(code)
                .map(this::withMerchantName)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Asset not found"));
    }

    @Transactional(readOnly = true)
    public Page<Asset> getMyAssets(String merchantUsername, int page, int size) {
        return assetDaoService.findByMerchant(merchantUsername, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100)))
                .map(this::withMerchantName);
    }

    private Asset withMerchantName(AssetEntity asset) {
        Asset dto = AssetDaoService.toDto(asset);
        dto.setMerchantName(merchantProfileDaoService.findByUserCode(asset.getMerchant().getCode())
                .map(MerchantProfileEntity::getBusinessName)
                .orElse(null));
        return dto;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
