package com.threelinelimited.rentalservice.service;

import com.threelinelimited.rentalservice.enums.AccountStatus;
import com.threelinelimited.rentalservice.model.MerchantProfileEntity;
import com.threelinelimited.rentalservice.repository.MerchantProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
