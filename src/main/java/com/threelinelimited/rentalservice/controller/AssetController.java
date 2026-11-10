package com.threelinelimited.rentalservice.controller;

import com.threelinelimited.rentalservice.dto.Asset;
import com.threelinelimited.rentalservice.dto.request.CreateAssetRequest;
import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/assets")
public class AssetController {
    private static final String MERCHANT_ONLY = "hasAuthority('" + UserRoleType.Code.ROLE_MERCHANT + "')";

    private final AssetService assetService;

    @PostMapping
    @PreAuthorize(MERCHANT_ONLY)
    public ResponseEntity<Asset> create(Authentication auth, @Valid @RequestBody CreateAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.createAsset(auth.getName(), request));
    }

    @GetMapping("/mine")
    @PreAuthorize(MERCHANT_ONLY)
    public ResponseEntity<Page<Asset>> myAssets(Authentication auth,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(assetService.getMyAssets(auth.getName(), page, size));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Asset> get(@PathVariable String code) {
        return ResponseEntity.ok(assetService.getAsset(code));
    }
}
