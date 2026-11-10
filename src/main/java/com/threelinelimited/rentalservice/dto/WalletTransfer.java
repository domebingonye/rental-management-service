package com.threelinelimited.rentalservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletTransfer {
    public enum Direction { SENT, RECEIVED }

    private String reference;
    private Direction direction;
    private BigDecimal amount;
    private String currency;
    private String senderWalletNumber;
    private String senderName;
    private String recipientWalletNumber;
    private String recipientName;
    private String narration;
    private BigDecimal balanceAfter;
    private LocalDateTime createdOn;
}
