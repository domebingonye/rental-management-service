package com._line.rentalservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/** One wallet per customer / merchant, created during onboarding. Staff do not get wallets. */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "WALLETS")
public class WalletEntity extends AuditModel {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private SystemUserEntity user;

    /** Public 10-digit identifier shown to the user. */
    @Column(name = "WALLET_NUMBER", nullable = false, unique = true, length = 10)
    private String walletNumber;

    /** Money is always BigDecimal with 2 decimal places — never double. */
    @Column(name = "BALANCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    /** Optimistic-lock version: a second safety net on top of the row lock taken when the balance changes. */
    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;
}
