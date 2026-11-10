package com.threelinelimited.rentalservice.dto;

import com.threelinelimited.rentalservice.enums.RentalStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Rental {
    private String reference;
    private String assetCode;
    private String assetName;
    private int quantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private int days;
    private BigDecimal dailyRate;
    private BigDecimal totalAmount;
    private String currency;
    private RentalStatus status;
    private String customerCode;
    private String customerName;
    private String merchantCode;
    private String merchantName;
    private BigDecimal walletBalanceAfter;
    private LocalDateTime createdOn;
}
