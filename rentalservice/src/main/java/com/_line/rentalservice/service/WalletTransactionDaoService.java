package com._line.rentalservice.service;

import com._line.rentalservice.dto.WalletTransaction;
import com._line.rentalservice.model.WalletTransactionEntity;
import com._line.rentalservice.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Persistence-facing helpers for WALLET_TRANSACTIONS (the ledger) and entity → DTO mapping. */
@Service
@RequiredArgsConstructor
public class WalletTransactionDaoService {
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletTransactionEntity save(WalletTransactionEntity entity) {
        return walletTransactionRepository.save(entity);
    }

    public Optional<WalletTransactionEntity> findByReference(String reference) {
        return walletTransactionRepository.findByReference(reference);
    }

    /** Newest first. */
    public Page<WalletTransactionEntity> findByWalletId(Long walletId, Pageable pageable) {
        return walletTransactionRepository.findByWalletIdOrderByIdDesc(walletId, pageable);
    }

    public static WalletTransaction toDto(WalletTransactionEntity t) {
        return WalletTransaction.builder()
                .reference(t.getReference())
                .walletNumber(t.getWallet().getWalletNumber())
                .type(t.getType())
                .amount(t.getAmount())
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .narration(t.getNarration())
                .createdOn(t.getCreatedOn())
                .build();
    }
}
