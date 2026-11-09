package com._line.rentalservice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum UserRoleType {
    CUSTOMER(Code.ROLE_CUSTOMER),
    MERCHANT(Code.ROLE_MERCHANT),
    STAFF(Code.ROLE_STAFF);

    @Getter
    private final String userType;

    public static class Code {
        public static final String ROLE_CUSTOMER = "CUSTOMER";
        public static final String ROLE_MERCHANT = "MERCHANT";
        public static final String ROLE_STAFF = "STAFF";

        private Code() {}
    }
}
