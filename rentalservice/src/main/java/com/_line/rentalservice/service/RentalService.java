package com._line.rentalservice.service;

import com._line.rentalservice.dto.Rental;
import com._line.rentalservice.dto.request.RentAssetRequest;
import com._line.rentalservice.enums.AccountStatus;
import com._line.rentalservice.enums.AssetStatus;
import com._line.rentalservice.enums.RentalStatus;
import com._line.rentalservice.enums.ResponseCodes;
import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.exception.RequestException;
import com._line.rentalservice.model.AssetEntity;
import com._line.rentalservice.model.MerchantProfileEntity;
import com._line.rentalservice.model.RentalEntity;
import com._line.rentalservice.model.SystemUserEntity;
import com._line.rentalservice.model.WalletEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RentalService {
    private static final int MAX_RENTAL_DAYS = 365;

    private final RentalDaoService rentalDaoService;
    private final AssetDaoService assetDaoService;
    private final SystemUserDaoService systemUserDaoService;
    private final WalletDaoService walletDaoService;
    private final MerchantProfileDaoService merchantProfileDaoService;
    private final WalletService walletService;

    @Transactional
    public Rental rentAsset(String customerUsername, RentAssetRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "End date cannot be before start date");
        }
        int days = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (days > MAX_RENTAL_DAYS) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "A rental cannot exceed " + MAX_RENTAL_DAYS + " days");
        }
        SystemUserEntity customer = systemUserDaoService.findByUsername(customerUsername)
                .filter(u -> u.getUserRoleType() == UserRoleType.CUSTOMER)
                .orElseThrow(() -> new RequestException(ResponseCodes.FORBIDDEN, "Only customers can rent assets"));
        String reference = StringUtils.hasText(request.getReference())
                ? request.getReference()
                : "RNT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        AssetEntity asset = assetDaoService.findByCodeForUpdate(request.getAssetCode())
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Asset not found"));

        var existing = rentalDaoService.findByReference(reference);
        if (existing.isPresent()) {
            if (!existing.get().getCustomer().getId().equals(customer.getId())) {
                throw new RequestException(ResponseCodes.CONFLICT, "Reference has already been used");
            }
            log.info("Duplicate rental {} from {} ignored", reference, customer.getCode());
            return toDto(existing.get(), walletDaoService.findByUsername(customerUsername).map(WalletEntity::getBalance).orElse(null));
        }

        SystemUserEntity merchant = asset.getMerchant();
        if (asset.getStatus() != AssetStatus.AVAILABLE || merchant.getStatus() != AccountStatus.ACTIVE) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Asset is not available for rent");
        }
        if (request.getQuantity() > asset.getQuantity()) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Only " + asset.getQuantity() + " unit(s) available");
        }

        BigDecimal total = asset.getDailyRate()
                .multiply(BigDecimal.valueOf(days))
                .multiply(BigDecimal.valueOf(request.getQuantity()))
                .setScale(2, RoundingMode.UNNECESSARY);

        WalletEntity customerWallet = walletDaoService.findByUsername(customerUsername)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Wallet not found"));
        WalletEntity merchantWallet = walletDaoService.findByUsername(merchant.getUsername())
                .orElseThrow(() -> new RequestException(ResponseCodes.BAD_REQUEST, "Merchant cannot receive payments"));
        walletService.lockInOrder(customerWallet, merchantWallet);

        String customerName = customer.getFirstName() + " " + customer.getSurname();
        String period = request.getQuantity() + " x " + days + " day(s)";
        walletService.moveFunds(customerWallet, merchantWallet, total, reference,
                "Rental: " + asset.getName() + " (" + period + ")",
                "Rental income: " + asset.getName() + " (" + period + ") from " + customerName);

        int remaining = asset.getQuantity() - request.getQuantity();
        asset.setQuantity(remaining);
        if (remaining == 0) {
            asset.setStatus(AssetStatus.UNAVAILABLE);
        }

        RentalEntity rental = rentalDaoService.save(new RentalEntity()
                .setReference(reference)
                .setAsset(asset)
                .setCustomer(customer)
                .setMerchant(merchant)
                .setQuantity(request.getQuantity())
                .setStartDate(request.getStartDate())
                .setEndDate(request.getEndDate())
                .setDays(days)
                .setDailyRate(asset.getDailyRate())
                .setTotalAmount(total)
                .setCurrency(asset.getCurrency())
                .setStatus(RentalStatus.ACTIVE));

        log.info("Rental {}: customer {} rented {} x {} ({} - {}) for {} {}", reference, customer.getCode(),
                request.getQuantity(), asset.getCode(), request.getStartDate(), request.getEndDate(), total, asset.getCurrency());
        return toDto(rental, customerWallet.getBalance());
    }

    @Transactional(readOnly = true)
    public Page<Rental> getMyRentals(String username, int page, int size) {
        SystemUserEntity user = systemUserDaoService.findByUsername(username)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "User not found"));
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100));
        var rentals = user.getUserRoleType() == UserRoleType.MERCHANT
                ? rentalDaoService.findByMerchant(username, pageable)
                : rentalDaoService.findByCustomer(username, pageable);
        return rentals.map(r -> toDto(r, null));
    }

    @Transactional(readOnly = true)
    public Rental getRental(String username, String reference) {
        return rentalDaoService.findByReference(reference)
                .filter(r -> r.getCustomer().getUsername().equals(username) || r.getMerchant().getUsername().equals(username))
                .map(r -> toDto(r, null))
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Rental not found"));
    }

    private Rental toDto(RentalEntity rental, BigDecimal customerBalanceAfter) {
        String merchantName = merchantProfileDaoService.findByUserCode(rental.getMerchant().getCode())
                .map(MerchantProfileEntity::getBusinessName)
                .orElse(null);
        return RentalDaoService.toDto(rental, merchantName, customerBalanceAfter);
    }
}
