package com._line.rentalservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * A bank account number dedicated to one wallet. Money "transferred" to this account number is credited
 * to the wallet. With a real provider (Paystack, Monnify, Flutterwave…) the number would be issued by the
 * provider; here it is generated in-house.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "VIRTUAL_ACCOUNTS")
public class VirtualAccountEntity extends AuditModel {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "WALLET_ID", nullable = false, unique = true)
    private WalletEntity wallet;

    /** 10-digit NUBAN-style number (9-digit serial + check digit). */
    @Column(name = "ACCOUNT_NUMBER", nullable = false, unique = true, length = 10)
    private String accountNumber;

    @Column(name = "ACCOUNT_NAME", nullable = false, length = 150)
    private String accountName;

    @Column(name = "BANK_NAME", nullable = false, length = 100)
    private String bankName;

    @Column(name = "BANK_CODE", nullable = false, length = 3)
    private String bankCode;
}
