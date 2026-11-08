package com._line.rentalservice.enums;

/** Our side's view of a transfer into a virtual account. */
public enum InboundTransferStatus {
    /** Webhook received; waiting for the provider to confirm the transfer settled. */
    PENDING_VERIFICATION,
    /** Provider confirmed SUCCESSFUL and the wallet was credited. Final. */
    CREDITED,
    /** Provider says the transfer failed; nothing credited. Final. */
    FAILED,
    /** Provider does not know the transfer, or it was not for one of our accounts; nothing credited. Final. */
    REJECTED;

    public boolean isFinal() {
        return this != PENDING_VERIFICATION;
    }
}
