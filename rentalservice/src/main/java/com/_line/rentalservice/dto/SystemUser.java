package com._line.rentalservice.dto;

import com._line.rentalservice.enums.AccountStatus;
import com._line.rentalservice.enums.UserRoleType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemUser {
    private String code;
    private String email;
    private String username;
    private String firstName;
    private String surname;
    private String phoneNumber;
    private UserRoleType userRoleType;
    private AccountStatus status;
    private MerchantDetails merchant;
    private Wallet wallet;
    private VirtualAccount virtualAccount;

    @Data
    @Builder
    public static class MerchantDetails {
        private String businessName;
        private String businessAddress;
        private String registrationNumber;
        private String approvedBy;
    }
}
