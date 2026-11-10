package com.threelinelimited.rentalservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class Wallet {
    private String walletNumber;
    private BigDecimal balance;
    private String currency;
    private String ownerCode;
}
