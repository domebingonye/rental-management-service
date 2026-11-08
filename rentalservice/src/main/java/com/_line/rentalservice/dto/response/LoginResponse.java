package com._line.rentalservice.dto.response;

import com._line.rentalservice.enums.UserRoleType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String token;
    private Long expiresInSeconds;
    private String code;
    private String email;
    private String username;
    private String firstName;
    private String surname;
    private UserRoleType userRoleType;
    private Boolean active;
    private List<String> authorities;
}
