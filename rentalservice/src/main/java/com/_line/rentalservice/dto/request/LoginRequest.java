package com._line.rentalservice.dto.request;

import com._line.rentalservice.enums.UserRoleType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username is required.")
    private String username;

    @NotBlank(message = "Password is required.")
    private String password;

    private UserRoleType userRoleType;
}
