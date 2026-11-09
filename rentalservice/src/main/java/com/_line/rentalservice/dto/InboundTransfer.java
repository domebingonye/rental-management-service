package com._line.rentalservice.dto;

import com._line.rentalservice.enums.InboundTransferStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InboundTransfer {
    private String sessionId;
    private String accountNumber;
    private BigDecimal amount;
    private String senderName;
    private String senderAccountNumber;
    private String senderBankName;
    private InboundTransferStatus status;
    private String statusReason;
    private LocalDateTime receivedOn;
    private LocalDateTime verifiedOn;
}
