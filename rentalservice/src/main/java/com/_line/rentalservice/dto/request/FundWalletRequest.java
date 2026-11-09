package com._line.rentalservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundWalletRequest {
    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00.")
    @DecimalMax(value = "10000000.00", message = "Amount cannot exceed 10,000,000.00 per transaction.")
    @Digits(integer = 17, fraction = 2, message = "Amount can have at most 2 decimal places.")
    private BigDecimal amount;

    @Size(min = 8, max = 64, message = "Reference must be 8-64 characters.")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Reference may only contain letters, digits, '_' and '-'.")
    private String reference;

    @Size(max = 255, message = "Narration is too long.")
    private String narration;
}
