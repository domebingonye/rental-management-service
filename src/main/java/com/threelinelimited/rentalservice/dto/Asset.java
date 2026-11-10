package com.threelinelimited.rentalservice.dto;

import com.threelinelimited.rentalservice.enums.AssetCategory;
import com.threelinelimited.rentalservice.enums.AssetStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Asset {
    private String code;
    private String name;
    private String description;
    private AssetCategory category;
    private BigDecimal dailyRate;
    private String currency;
    private int quantity;
    private String location;
    private AssetStatus status;
    private String merchantCode;
    private String merchantName;
    private LocalDateTime createdOn;
}
