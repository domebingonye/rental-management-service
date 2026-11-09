package com._line.rentalservice.service;

import com._line.rentalservice.dto.Rental;
import com._line.rentalservice.model.RentalEntity;
import com._line.rentalservice.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RentalDaoService {
    private final RentalRepository rentalRepository;

    public RentalEntity save(RentalEntity entity) {
        return rentalRepository.save(entity);
    }

    public Optional<RentalEntity> findByReference(String reference) {
        return rentalRepository.findByReference(reference);
    }

    public Page<RentalEntity> findByCustomer(String username, Pageable pageable) {
        return rentalRepository.findByCustomerUsernameOrderByIdDesc(username, pageable);
    }

    public Page<RentalEntity> findByMerchant(String username, Pageable pageable) {
        return rentalRepository.findByMerchantUsernameOrderByIdDesc(username, pageable);
    }

    public static Rental toDto(RentalEntity r, String merchantName, BigDecimal customerBalanceAfter) {
        return Rental.builder()
                .reference(r.getReference())
                .assetCode(r.getAsset().getCode())
                .assetName(r.getAsset().getName())
                .quantity(r.getQuantity())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .days(r.getDays())
                .dailyRate(r.getDailyRate())
                .totalAmount(r.getTotalAmount())
                .currency(r.getCurrency())
                .status(r.getStatus())
                .customerCode(r.getCustomer().getCode())
                .customerName(r.getCustomer().getFirstName() + " " + r.getCustomer().getSurname())
                .merchantCode(r.getMerchant().getCode())
                .merchantName(merchantName)
                .walletBalanceAfter(customerBalanceAfter)
                .createdOn(r.getCreatedOn())
                .build();
    }
}
