package com.threelinelimited.rentalservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VirtualAccount {
    private String accountNumber;
    private String accountName;
    private String bankName;
    private String bankCode;
    private String walletNumber;
}
