package com._line.rentalservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletTransferRequest {
    @NotBlank(message = "Recipient wallet number is required.")
    @Pattern(regexp = "^\\d{10}$", message = "Recipient wallet number must be 10 digits.")
    private String recipientWalletNumber;

    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00.")
    @DecimalMax(value = "5000000.00", message = "Amount cannot exceed 5,000,000.00 per transfer.")
    @Digits(integer = 17, fraction = 2, message = "Amount can have at most 2 decimal places.")
    private BigDecimal amount;

    @Size(min = 8, max = 60, message = "Reference must be 8-60 characters.")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Reference may only contain letters, digits, '_' and '-'.")
    private String reference;

    @Size(max = 200, message = "Narration is too long.")
    private String narration;
}
