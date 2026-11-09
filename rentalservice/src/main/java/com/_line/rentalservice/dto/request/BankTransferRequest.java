package com._line.rentalservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BankTransferRequest {
    @NotBlank(message = "Source bank name is required.")
    @Size(max = 100, message = "Source bank name is too long.")
    private String sourceBankName;

    @NotBlank(message = "Source account number is required.")
    @Pattern(regexp = "^\\d{10}$", message = "Source account number must be 10 digits.")
    private String sourceAccountNumber;

    @NotBlank(message = "Source account name is required.")
    @Size(max = 150, message = "Source account name is too long.")
    private String sourceAccountName;

    @NotBlank(message = "Destination account number is required.")
    @Pattern(regexp = "^\\d{10}$", message = "Destination account number must be 10 digits.")
    private String destinationAccountNumber;

    @NotBlank(message = "Destination bank code is required.")
    @Pattern(regexp = "^\\d{3}$", message = "Destination bank code must be 3 digits.")
    private String destinationBankCode;

    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00.")
    @DecimalMax(value = "10000000.00", message = "Amount cannot exceed 10,000,000.00 per transaction.")
    @Digits(integer = 17, fraction = 2, message = "Amount can have at most 2 decimal places.")
    private BigDecimal amount;

    @Size(max = 100, message = "Narration is too long.")
    private String narration;
}
