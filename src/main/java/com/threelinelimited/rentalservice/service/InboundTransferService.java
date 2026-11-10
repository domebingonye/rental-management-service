package com.threelinelimited.rentalservice.service;

import com.threelinelimited.rentalservice.config.PropsReader;
import com.threelinelimited.rentalservice.dto.InboundTransfer;
import com.threelinelimited.rentalservice.dto.provider.ProviderTransaction;
import com.threelinelimited.rentalservice.dto.provider.ProviderWebhookEvent;
import com.threelinelimited.rentalservice.enums.InboundTransferStatus;
import com.threelinelimited.rentalservice.enums.ResponseCodes;
import com.threelinelimited.rentalservice.exception.RequestException;
import com.threelinelimited.rentalservice.model.InboundTransferEntity;
import com.threelinelimited.rentalservice.model.VirtualAccountEntity;
import com.threelinelimited.rentalservice.service.provider.PaymentProviderClient;
import com.threelinelimited.rentalservice.utils.HmacUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InboundTransferService {
    private final InboundTransferDaoService inboundTransferDaoService;
    private final VirtualAccountDaoService virtualAccountDaoService;
    private final WalletService walletService;
    private final PaymentProviderClient paymentProviderClient;
    private final PropsReader propsReader;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public InboundTransfer handleWebhook(String rawBody, String signature) {
        if (!HmacUtil.verify(rawBody, signature, propsReader.getProviderWebhookSecret())) {
            log.warn("Rejected webhook with invalid signature");
            throw new RequestException(ResponseCodes.UNAUTHORIZED, "Invalid signature");
        }

        ProviderWebhookEvent event = parse(rawBody);
        ProviderTransaction claim = event.getData();
        if (claim == null || !StringUtils.hasText(claim.getSessionId())) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Webhook has no session id");
        }
        log.info("Webhook {} received for session {}", event.getEvent(), claim.getSessionId());

        recordWebhook(claim);
        InboundTransferEntity record = inboundTransferDaoService.findBySessionId(claim.getSessionId()).orElseThrow();
        if (record.getStatus().isFinal()) {
            log.info("Session {} already {}; webhook acknowledged without action", record.getSessionId(), record.getStatus());
            return InboundTransferDaoService.toDto(record);
        }

        return verify(record.getSessionId());
    }

    public InboundTransfer verify(String sessionId) {
        InboundTransferEntity current = inboundTransferDaoService.findBySessionId(sessionId)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Inbound transfer not found"));
        if (current.getStatus().isFinal()) return InboundTransferDaoService.toDto(current);

        Optional<ProviderTransaction> providerTx = paymentProviderClient.requery(sessionId);

        return transactionTemplate.execute(status -> {
            InboundTransferEntity record = inboundTransferDaoService.findBySessionId(sessionId).orElseThrow();
            if (record.getStatus().isFinal()) return InboundTransferDaoService.toDto(record);

            if (providerTx.isEmpty()) {
                return finish(record, InboundTransferStatus.REJECTED, "Transaction not found at provider");
            }
            ProviderTransaction tx = providerTx.get();
            record.setSenderName(tx.getSourceAccountName())
                    .setSenderAccountNumber(tx.getSourceAccountNumber())
                    .setSenderBankName(tx.getSourceBankName());
            if (isForThisPlatform(tx)) {
                virtualAccountDaoService.findByAccountNumber(tx.getDestinationAccountNumber()).ifPresent(record::setVirtualAccount);
            }

            return switch (tx.getStatus()) {
                case PENDING -> {
                    record.setStatusReason("Awaiting settlement at provider");
                    yield InboundTransferDaoService.toDto(inboundTransferDaoService.save(record));
                }
                case FAILED -> finish(record, InboundTransferStatus.FAILED,
                        StringUtils.hasText(tx.getFailureReason()) ? tx.getFailureReason() : "Failed at provider");
                case SUCCESSFUL -> creditIfOurs(record, tx);
            };
        });
    }

    private InboundTransfer creditIfOurs(InboundTransferEntity record, ProviderTransaction tx) {
        if (!isForThisPlatform(tx)) {
            return finish(record, InboundTransferStatus.REJECTED, "Transfer is not for this platform");
        }
        Optional<VirtualAccountEntity> account = Optional.ofNullable(record.getVirtualAccount());
        if (account.isEmpty()) {
            return finish(record, InboundTransferStatus.REJECTED, "Destination virtual account not found");
        }

        boolean claimsMatch = tx.getAmount().compareTo(Objects.requireNonNullElse(record.getClaimedAmount(), tx.getAmount())) == 0
                && Objects.equals(tx.getDestinationAccountNumber(), Objects.requireNonNullElse(record.getClaimedAccountNumber(), tx.getDestinationAccountNumber()));
        if (!claimsMatch) {
            log.warn("Session {}: webhook claimed {} to {}, provider says {} to {}. Using provider values.",
                    record.getSessionId(), record.getClaimedAmount(), record.getClaimedAccountNumber(),
                    tx.getAmount(), tx.getDestinationAccountNumber());
        }

        VirtualAccountEntity va = account.get();
        String narration = "Transfer from " + tx.getSourceAccountName() + " (" + tx.getSourceBankName() + ")";
        walletService.credit(va.getWallet().getUser().getUsername(), tx.getAmount(), record.getSessionId(), narration);

        record.setVerifiedAmount(tx.getAmount());
        log.info("Session {} verified SUCCESSFUL; credited {} to wallet {}", record.getSessionId(), tx.getAmount(), va.getWallet().getWalletNumber());
        return finish(record, InboundTransferStatus.CREDITED,
                claimsMatch ? null : "Webhook details did not match provider; provider values used");
    }

    private boolean isForThisPlatform(ProviderTransaction tx) {
        return propsReader.getVirtualAccountBankCode().equals(tx.getDestinationBankCode())
                && propsReader.getWalletCurrency().equals(tx.getCurrency());
    }

    private InboundTransfer finish(InboundTransferEntity record, InboundTransferStatus status, String reason) {
        record.setStatus(status).setStatusReason(reason).setVerifiedOn(LocalDateTime.now());
        if (status != InboundTransferStatus.CREDITED) {
            log.info("Session {} closed as {}: {}", record.getSessionId(), status, reason);
        }
        return InboundTransferDaoService.toDto(inboundTransferDaoService.save(record));
    }

    public Page<InboundTransfer> getMyTransfers(String username, int page, int size) {
        return inboundTransferDaoService.findByUsername(username, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100)))
                .map(InboundTransferDaoService::toDto);
    }

    private InboundTransferEntity recordWebhook(ProviderTransaction claim) {
        Optional<InboundTransferEntity> existing = inboundTransferDaoService.findBySessionId(claim.getSessionId());
        if (existing.isPresent()) {
            InboundTransferEntity record = existing.get();
            record.setWebhookCount(record.getWebhookCount() + 1);
            return inboundTransferDaoService.save(record);
        }
        try {
            return inboundTransferDaoService.save(new InboundTransferEntity()
                    .setSessionId(claim.getSessionId())
                    .setClaimedAccountNumber(claim.getDestinationAccountNumber())
                    .setClaimedAmount(claim.getAmount())
                    .setStatus(InboundTransferStatus.PENDING_VERIFICATION)
                    .setWebhookCount(1));
        } catch (DataIntegrityViolationException e) {
            return inboundTransferDaoService.findBySessionId(claim.getSessionId()).orElseThrow(() -> e);
        }
    }

    private ProviderWebhookEvent parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, ProviderWebhookEvent.class);
        } catch (JsonProcessingException e) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Malformed webhook body");
        }
    }
}
