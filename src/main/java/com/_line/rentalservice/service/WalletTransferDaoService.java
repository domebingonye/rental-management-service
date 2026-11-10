package com._line.rentalservice.service;

import com._line.rentalservice.dto.WalletTransfer;
import com._line.rentalservice.model.WalletTransferEntity;
import com._line.rentalservice.repository.WalletTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletTransferDaoService {
    private final WalletTransferRepository walletTransferRepository;

    public WalletTransferEntity save(WalletTransferEntity entity) {
        return walletTransferRepository.save(entity);
    }

    public Optional<WalletTransferEntity> findByReference(String reference) {
        return walletTransferRepository.findByReference(reference);
    }

    public Page<WalletTransferEntity> findInvolving(String username, Pageable pageable) {
        return walletTransferRepository.findInvolving(username, pageable);
    }

    public static WalletTransfer toDto(WalletTransferEntity t, String viewerUsername, BigDecimal senderBalanceAfter) {
        boolean sent = t.getSenderWallet().getUser().getUsername().equals(viewerUsername);
        return WalletTransfer.builder()
                .reference(t.getReference())
                .direction(sent ? WalletTransfer.Direction.SENT : WalletTransfer.Direction.RECEIVED)
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .senderWalletNumber(t.getSenderWallet().getWalletNumber())
                .senderName(t.getSenderName())
                .recipientWalletNumber(t.getRecipientWallet().getWalletNumber())
                .recipientName(t.getRecipientName())
                .narration(t.getNarration())
                .balanceAfter(sent ? senderBalanceAfter : null)
                .createdOn(t.getCreatedOn())
                .build();
    }
}
