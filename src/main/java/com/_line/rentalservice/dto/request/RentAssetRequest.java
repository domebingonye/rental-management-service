package com._line.rentalservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RentAssetRequest {
    @NotBlank(message = "Asset code is required.")
    private String assetCode;

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    @Max(value = 1000, message = "Quantity cannot exceed 1,000.")
    private Integer quantity;

    @NotNull(message = "Start date is required.")
    @FutureOrPresent(message = "Start date cannot be in the past.")
    private LocalDate startDate;

    @NotNull(message = "End date is required.")
    private LocalDate endDate;

    @Size(min = 8, max = 60, message = "Reference must be 8-60 characters.")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Reference may only contain letters, digits, '_' and '-'.")
    private String reference;
}
