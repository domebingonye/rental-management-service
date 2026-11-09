package com._line.rentalservice.dto;

import com._line.rentalservice.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletTransaction {
    private String reference;
    private String walletNumber;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String narration;
    private LocalDateTime createdOn;
}
