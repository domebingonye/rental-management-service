package com._line.rentalservice.enums;

public enum TransactionType {
    /** Money added to the wallet (funding, refunds, rental payouts to merchants). */
    CREDIT,
    /** Money taken out of the wallet (rental payments, withdrawals). */
    DEBIT
}
