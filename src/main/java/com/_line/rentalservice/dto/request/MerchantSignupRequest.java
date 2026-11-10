package com._line.rentalservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantSignupRequest extends SignupRequest {
    @NotBlank(message = "Business name is required.")
    @Size(max = 150, message = "Business name is too long.")
    private String businessName;

    @Size(max = 255, message = "Business address is too long.")
    private String businessAddress;

    @Size(max = 50, message = "Registration number is too long.")
    private String registrationNumber;
}
