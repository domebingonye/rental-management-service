package com._line.rentalservice.service;

import com._line.rentalservice.enums.AccountStatus;
import com._line.rentalservice.model.MerchantProfileEntity;
import com._line.rentalservice.repository.MerchantProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** Persistence-facing helpers for MERCHANT_PROFILES. */
@Service
@RequiredArgsConstructor
public class MerchantProfileDaoService {
    private final MerchantProfileRepository merchantProfileRepository;

    public MerchantProfileEntity save(MerchantProfileEntity entity) {
        return merchantProfileRepository.save(entity);
    }

    public Optional<MerchantProfileEntity> findByUserCode(String userCode) {
        return merchantProfileRepository.findByUserCode(userCode);
    }

    public List<MerchantProfileEntity> findByUserStatus(AccountStatus status) {
        return merchantProfileRepository.findByUserStatusOrderByIdAsc(status);
    }
}
