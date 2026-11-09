package com._line.rentalservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferRecipient {
    private String walletNumber;
    private String name;
}
