package com._line.rentalservice.service;

import com._line.rentalservice.dto.InboundTransfer;
import com._line.rentalservice.model.InboundTransferEntity;
import com._line.rentalservice.repository.InboundTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Persistence-facing helpers for INBOUND_TRANSFERS and entity → DTO mapping. */
@Service
@RequiredArgsConstructor
public class InboundTransferDaoService {
    private final InboundTransferRepository inboundTransferRepository;

    public InboundTransferEntity save(InboundTransferEntity entity) {
        return inboundTransferRepository.save(entity);
    }

    public Optional<InboundTransferEntity> findBySessionId(String sessionId) {
        return inboundTransferRepository.findBySessionId(sessionId);
    }

    public Page<InboundTransferEntity> findByUsername(String username, Pageable pageable) {
        return inboundTransferRepository.findByVirtualAccountWalletUserUsernameOrderByIdDesc(username, pageable);
    }

    public static InboundTransfer toDto(InboundTransferEntity e) {
        return InboundTransfer.builder()
                .sessionId(e.getSessionId())
                .accountNumber(e.getVirtualAccount() != null ? e.getVirtualAccount().getAccountNumber() : e.getClaimedAccountNumber())
                .amount(e.getVerifiedAmount() != null ? e.getVerifiedAmount() : e.getClaimedAmount())
                .senderName(e.getSenderName())
                .senderAccountNumber(e.getSenderAccountNumber())
                .senderBankName(e.getSenderBankName())
                .status(e.getStatus())
                .statusReason(e.getStatusReason())
                .receivedOn(e.getCreatedOn())
                .verifiedOn(e.getVerifiedOn())
                .build();
    }
}
