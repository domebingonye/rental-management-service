package com._line.rentalservice.model;

import com._line.rentalservice.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * Immutable ledger entry. Every balance change writes one row, so the balance can always be
 * explained (and re-derived) from the history.
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "WALLET_TRANSACTIONS", indexes = @Index(name = "IX_WALLET_TX_WALLET", columnList = "WALLET_ID"))
public class WalletTransactionEntity extends AuditModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "WALLET_ID", nullable = false)
    private WalletEntity wallet;

    /** Unique per transaction; doubles as the idempotency key so a retried request is not applied twice. */
    @Column(name = "REFERENCE", nullable = false, unique = true, length = 64)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "BALANCE_BEFORE", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "BALANCE_AFTER", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "NARRATION")
    private String narration;
}
