package com.threelinelimited.rentalservice.service;

import com.threelinelimited.rentalservice.dto.provider.ProviderTransaction;
import com.threelinelimited.rentalservice.model.MockBankTransferEntity;
import com.threelinelimited.rentalservice.repository.MockBankTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MockBankTransferDaoService {
    private final MockBankTransferRepository mockBankTransferRepository;

    public MockBankTransferEntity save(MockBankTransferEntity entity) {
        return mockBankTransferRepository.save(entity);
    }

    public Optional<MockBankTransferEntity> findBySessionId(String sessionId) {
        return mockBankTransferRepository.findBySessionId(sessionId);
    }

    public static ProviderTransaction toProviderTransaction(MockBankTransferEntity e) {
        return ProviderTransaction.builder()
                .sessionId(e.getSessionId())
                .status(e.getStatus())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .sourceBankName(e.getSourceBankName())
                .sourceAccountNumber(e.getSourceAccountNumber())
                .sourceAccountName(e.getSourceAccountName())
                .destinationAccountNumber(e.getDestinationAccountNumber())
                .destinationAccountName(e.getDestinationAccountName())
                .destinationBankCode(e.getDestinationBankCode())
                .narration(e.getNarration())
                .failureReason(e.getFailureReason())
                .initiatedOn(e.getCreatedOn())
                .completedOn(e.getCompletedOn())
                .build();
    }
}
