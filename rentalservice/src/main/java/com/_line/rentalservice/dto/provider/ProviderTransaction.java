package com._line.rentalservice.dto.provider;

import com._line.rentalservice.enums.BankTransferStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderTransaction {
    private String sessionId;
    private BankTransferStatus status;
    private BigDecimal amount;
    private String currency;
    private String sourceBankName;
    private String sourceAccountNumber;
    private String sourceAccountName;
    private String destinationAccountNumber;
    private String destinationAccountName;
    private String destinationBankCode;
    private String narration;
    private String failureReason;
    private LocalDateTime initiatedOn;
    private LocalDateTime completedOn;
}
