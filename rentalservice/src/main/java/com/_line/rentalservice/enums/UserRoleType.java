package com._line.rentalservice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The three kinds of actors on the rental platform. One account has exactly one role,
 * and the role name is used directly as the Spring Security authority (as in galaxy-auth-service).
 */
@AllArgsConstructor
public enum UserRoleType {
    CUSTOMER(Code.ROLE_CUSTOMER),
    MERCHANT(Code.ROLE_MERCHANT),
    STAFF(Code.ROLE_STAFF);

    @Getter
    private final String userType;

    /** Compile-time constants so they can be used inside {@code @PreAuthorize}. */
    public static class Code {
        public static final String ROLE_CUSTOMER = "CUSTOMER";
        public static final String ROLE_MERCHANT = "MERCHANT";
        public static final String ROLE_STAFF = "STAFF";

        private Code() {}
    }
}
