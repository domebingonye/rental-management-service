package com._line.rentalservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "WALLET_TRANSFERS", indexes = {
        @Index(name = "IX_WALLET_TRANSFERS_SENDER", columnList = "SENDER_WALLET_ID"),
        @Index(name = "IX_WALLET_TRANSFERS_RECIPIENT", columnList = "RECIPIENT_WALLET_ID")})
public class WalletTransferEntity extends AuditModel {

    /** Client-supplied or generated; unique, and the idempotency key for retries. */
    @Column(name = "REFERENCE", nullable = false, unique = true, length = 60)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SENDER_WALLET_ID", nullable = false)
    private WalletEntity senderWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RECIPIENT_WALLET_ID", nullable = false)
    private WalletEntity recipientWallet;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Column(name = "SENDER_NAME", nullable = false, length = 150)
    private String senderName;

    @Column(name = "RECIPIENT_NAME", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "NARRATION")
    private String narration;
}
