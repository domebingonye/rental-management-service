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

/** Persistence-facing helpers for WALLETS: lookups, locking, wallet-number generation and entity → DTO mapping. */
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

    /** Takes a row lock held until the surrounding transaction commits. Must be called inside a transaction. */
    public Optional<WalletEntity> findByUsernameForUpdate(String username) {
        return walletRepository.findByUsernameForUpdate(username);
    }

    /**
     * Takes a row lock on a wallet that is ALREADY loaded in this transaction and re-reads it.
     * <p>
     * A plain "SELECT ... FOR UPDATE" query would hand back the instance already in the persistence
     * context with the balance read <em>before</em> the lock — a lost-update bug. refresh() overwrites it
     * with the locked, current row. Must be called inside a transaction.
     */
    public WalletEntity lockAndRefresh(WalletEntity wallet) {
        entityManager.refresh(wallet, LockModeType.PESSIMISTIC_WRITE);
        return wallet;
    }

    /** Includes the owning user. */
    public Optional<WalletEntity> findByWalletNumber(String walletNumber) {
        return walletRepository.findByWalletNumber(walletNumber);
    }

    /** 10 digits, never starting with 0 so it survives being treated as a number by clients. */
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
