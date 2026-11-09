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
@Table(name = "WALLETS")
public class WalletEntity extends AuditModel {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private SystemUserEntity user;

    @Column(name = "WALLET_NUMBER", nullable = false, unique = true, length = 10)
    private String walletNumber;

    @Column(name = "BALANCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;
}
