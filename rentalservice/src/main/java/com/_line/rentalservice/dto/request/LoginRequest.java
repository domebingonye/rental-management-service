package com._line.rentalservice.dto.request;

import com._line.rentalservice.enums.UserRoleType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    /** Username or email. */
    @NotBlank(message = "Username is required.")
    private String username;

    @NotBlank(message = "Password is required.")
    private String password;

    /**
     * Optional: the portal the user is logging in through (customer app, merchant portal, staff back-office).
     * When supplied, login is rejected if the account does not hold that role.
     */
    private UserRoleType userRoleType;
}
