package com._line.rentalservice.dto;

import lombok.Builder;
import lombok.Data;

/** What the customer sees before confirming a transfer ("You are paying ACME RENTALS"). */
@Data
@Builder
public class TransferRecipient {
    private String walletNumber;
    private String name;
}
