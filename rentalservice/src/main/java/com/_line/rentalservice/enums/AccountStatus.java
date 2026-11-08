package com._line.rentalservice.enums;

public enum AccountStatus {
    /** Can log in. */
    ACTIVE,
    /** Merchant registered but not yet approved by staff. Cannot log in. */
    PENDING_APPROVAL,
    /** Blocked by staff. Cannot log in. */
    SUSPENDED
}
