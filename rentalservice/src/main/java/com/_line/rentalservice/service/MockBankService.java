package com._line.rentalservice.service;

import com._line.rentalservice.config.PropsReader;
import com._line.rentalservice.constant.ApplicationConstants;
import com._line.rentalservice.dto.provider.NameEnquiryResponse;
import com._line.rentalservice.dto.provider.ProviderTransaction;
import com._line.rentalservice.dto.provider.ProviderWebhookEvent;
import com._line.rentalservice.dto.request.BankTransferRequest;
import com._line.rentalservice.enums.BankTransferStatus;
import com._line.rentalservice.enums.ResponseCodes;
import com._line.rentalservice.exception.RequestException;
import com._line.rentalservice.model.MockBankTransferEntity;
import com._line.rentalservice.model.VirtualAccountEntity;
import com._line.rentalservice.utils.HmacUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class MockBankService {
    private static final String FAILING_ACCOUNT_SUFFIX = "0000";
    private static final String SENDER_BANK_CODE = "999058";
    private static final DateTimeFormatter SESSION_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MockBankTransferDaoService mockBankTransferDaoService;
    private final VirtualAccountDaoService virtualAccountDaoService;
    private final PropsReader propsReader;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final TaskScheduler scheduler;
    private final RestClient restClient = RestClient.create();

    public MockBankService(MockBankTransferDaoService mockBankTransferDaoService,
                           VirtualAccountDaoService virtualAccountDaoService,
                           PropsReader propsReader, ObjectMapper objectMapper, Environment environment,
                           @Qualifier("mockBankScheduler") TaskScheduler scheduler) {
        this.mockBankTransferDaoService = mockBankTransferDaoService;
        this.virtualAccountDaoService = virtualAccountDaoService;
        this.propsReader = propsReader;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.scheduler = scheduler;
    }

    public NameEnquiryResponse nameEnquiry(String accountNumber, String bankCode) {
        VirtualAccountEntity account = lookupDestination(accountNumber, bankCode);
        return NameEnquiryResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .bankCode(account.getBankCode())
                .bankName(account.getBankName())
                .build();
    }

    public ProviderTransaction initiateTransfer(BankTransferRequest request) {
        VirtualAccountEntity destination = lookupDestination(request.getDestinationAccountNumber(), request.getDestinationBankCode());

        MockBankTransferEntity transfer = mockBankTransferDaoService.save(new MockBankTransferEntity()
                .setSessionId(newSessionId())
                .setSourceBankName(request.getSourceBankName().trim())
                .setSourceAccountNumber(request.getSourceAccountNumber())
                .setSourceAccountName(request.getSourceAccountName().trim())
                .setDestinationAccountNumber(destination.getAccountNumber())
                .setDestinationAccountName(destination.getAccountName())
                .setDestinationBankCode(destination.getBankCode())
                .setAmount(request.getAmount().setScale(2, RoundingMode.UNNECESSARY))
                .setCurrency(propsReader.getWalletCurrency())
                .setNarration(request.getNarration())
                .setStatus(BankTransferStatus.PENDING));

        log.info("[MOCK BANK] Transfer {} of {} from {} to {} accepted (PENDING)", transfer.getSessionId(),
                transfer.getAmount(), transfer.getSourceAccountNumber(), transfer.getDestinationAccountNumber());

        String sessionId = transfer.getSessionId();
        scheduler.schedule(() -> settle(sessionId), Instant.now().plusMillis(propsReader.getProviderSettlementDelayMs()));
        return MockBankTransferDaoService.toProviderTransaction(transfer);
    }

    public ProviderTransaction getTransaction(String authorizationHeader, String sessionId) {
        String key = authorizationHeader != null && authorizationHeader.startsWith(ApplicationConstants.BEARER_PREFIX)
                ? authorizationHeader.substring(ApplicationConstants.BEARER_PREFIX.length()).trim() : null;
        if (!HmacUtil.constantTimeEquals(key, propsReader.getProviderSecretKey())) {
            throw new RequestException(ResponseCodes.UNAUTHORIZED, "Invalid API key");
        }
        return mockBankTransferDaoService.findBySessionId(sessionId)
                .map(MockBankTransferDaoService::toProviderTransaction)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Transaction not found"));
    }

    public ProviderTransaction getTransferReceipt(String sessionId) {
        return mockBankTransferDaoService.findBySessionId(sessionId)
                .map(MockBankTransferDaoService::toProviderTransaction)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Transaction not found"));
    }

    void settle(String sessionId) {
        try {
            MockBankTransferEntity transfer = mockBankTransferDaoService.findBySessionId(sessionId).orElseThrow();
            if (transfer.getStatus() != BankTransferStatus.PENDING) return;

            if (transfer.getSourceAccountNumber().endsWith(FAILING_ACCOUNT_SUFFIX)) {
                transfer.setStatus(BankTransferStatus.FAILED).setFailureReason("Insufficient funds");
            } else {
                transfer.setStatus(BankTransferStatus.SUCCESSFUL);
            }
            transfer.setCompletedOn(LocalDateTime.now());
            mockBankTransferDaoService.save(transfer);
            log.info("[MOCK BANK] Transfer {} settled: {}", sessionId, transfer.getStatus());

            deliverWebhook(sessionId);
        } catch (Exception e) {
            log.error("[MOCK BANK] Settlement of {} failed", sessionId, e);
        }
    }

    private void deliverWebhook(String sessionId) {
        MockBankTransferEntity transfer = mockBankTransferDaoService.findBySessionId(sessionId).orElseThrow();
        if (transfer.isWebhookDelivered()) return;

        int attempt = transfer.getWebhookAttempts() + 1;
        transfer.setWebhookAttempts(attempt);
        boolean delivered = false;
        try {
            String body = objectMapper.writeValueAsString(ProviderWebhookEvent.builder()
                    .event(transfer.getStatus() == BankTransferStatus.SUCCESSFUL
                            ? ProviderWebhookEvent.TRANSFER_SUCCESSFUL : ProviderWebhookEvent.TRANSFER_FAILED)
                    .data(MockBankTransferDaoService.toProviderTransaction(transfer))
                    .build());
            restClient.post()
                    .uri(webhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(ApplicationConstants.PROVIDER_SIGNATURE_HEADER, HmacUtil.sign(body, propsReader.getProviderWebhookSecret()))
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            delivered = true;
            log.info("[MOCK BANK] Webhook for {} delivered (attempt {})", sessionId, attempt);
        } catch (JsonProcessingException e) {
            log.error("[MOCK BANK] Could not serialise webhook for {}", sessionId, e);
        } catch (Exception e) {
            log.warn("[MOCK BANK] Webhook for {} failed (attempt {}): {}", sessionId, attempt, e.getMessage());
        }

        transfer.setWebhookDelivered(delivered);
        mockBankTransferDaoService.save(transfer);

        if (!delivered && attempt < propsReader.getProviderWebhookMaxAttempts()) {
            long delay = propsReader.getProviderWebhookRetryDelayMs() * (1L << (attempt - 1));
            scheduler.schedule(() -> deliverWebhook(sessionId), Instant.now().plusMillis(delay));
        } else if (!delivered) {
            log.error("[MOCK BANK] Giving up on webhook for {} after {} attempts", sessionId, attempt);
        }
    }

    private VirtualAccountEntity lookupDestination(String accountNumber, String bankCode) {
        if (!propsReader.getVirtualAccountBankCode().equals(bankCode)) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Unknown destination bank");
        }
        return virtualAccountDaoService.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RequestException(ResponseCodes.BAD_REQUEST, "Destination account not found"));
    }

    private String webhookUrl() {
        String url = environment.getProperty("payment.provider.webhook-url");
        if (!StringUtils.hasText(url)) throw new IllegalStateException("payment.provider.webhook-url is not set");
        return url;
    }

    private static String newSessionId() {
        StringBuilder sb = new StringBuilder(SENDER_BANK_CODE).append(LocalDateTime.now().format(SESSION_TIME));
        for (int i = 0; i < 12; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }
}
