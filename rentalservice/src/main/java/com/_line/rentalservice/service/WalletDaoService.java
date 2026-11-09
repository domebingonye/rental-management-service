package com._line.rentalservice.service;

import com._line.rentalservice.dto.Wallet;
import com._line.rentalservice.model.WalletEntity;
import com._line.rentalservice.repository.WalletRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletDaoService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WalletRepository walletRepository;
    private final EntityManager entityManager;

    public WalletEntity save(WalletEntity entity) {
        return walletRepository.save(entity);
    }

    public Optional<WalletEntity> findByUsername(String username) {
        return walletRepository.findByUserUsername(username);
    }

    public Optional<WalletEntity> findByUsernameForUpdate(String username) {
        return walletRepository.findByUsernameForUpdate(username);
    }

    public WalletEntity lockAndRefresh(WalletEntity wallet) {
        entityManager.refresh(wallet, LockModeType.PESSIMISTIC_WRITE);
        return wallet;
    }

    public Optional<WalletEntity> findByWalletNumber(String walletNumber) {
        return walletRepository.findByWalletNumber(walletNumber);
    }

    public String generateWalletNumber() {
        String number;
        do {
            number = String.valueOf(1_000_000_000L + (long) (RANDOM.nextDouble() * 9_000_000_000L));
        } while (walletRepository.existsByWalletNumber(number));
        return number;
    }

    public static Wallet toDto(WalletEntity w) {
        return Wallet.builder()
                .walletNumber(w.getWalletNumber())
                .balance(w.getBalance())
                .currency(w.getCurrency())
                .ownerCode(w.getUser().getCode())
                .build();
    }
}
