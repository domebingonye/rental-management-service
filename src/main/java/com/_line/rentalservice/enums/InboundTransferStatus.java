package com._line.rentalservice.enums;

public enum InboundTransferStatus {
    PENDING_VERIFICATION,
    CREDITED,
    FAILED,
    REJECTED;

    public boolean isFinal() {
        return this != PENDING_VERIFICATION;
    }
}
