package com.threelinelimited.rentalservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank(message = "Email is required.")
    @Email(message = "Email is invalid.")
    private String email;

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 80, message = "Username must be 3-80 characters.")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username may only contain letters, digits, '.', '_' and '-'.")
    private String username;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters.")
    private String password;

    @NotBlank(message = "First name is required.")
    private String firstName;

    @NotBlank(message = "Surname is required.")
    private String surname;

    @Size(max = 20, message = "Phone number is too long.")
    private String phoneNumber;
}
