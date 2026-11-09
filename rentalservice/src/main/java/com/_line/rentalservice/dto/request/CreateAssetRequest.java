package com._line.rentalservice.dto.request;

import com._line.rentalservice.enums.AssetCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAssetRequest {
    @NotBlank(message = "Name is required.")
    @Size(max = 150, message = "Name is too long.")
    private String name;

    @Size(max = 1000, message = "Description is too long.")
    private String description;

    @NotNull(message = "Category is required.")
    private AssetCategory category;

    @NotNull(message = "Daily rate is required.")
    @DecimalMin(value = "1.00", message = "Daily rate must be at least 1.00.")
    @DecimalMax(value = "10000000.00", message = "Daily rate cannot exceed 10,000,000.00.")
    @Digits(integer = 17, fraction = 2, message = "Daily rate can have at most 2 decimal places.")
    private BigDecimal dailyRate;

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    @Max(value = 10000, message = "Quantity cannot exceed 10,000.")
    private Integer quantity;

    @Size(max = 150, message = "Location is too long.")
    private String location;
}
