package com.threelinelimited.rentalservice.model;

import com.threelinelimited.rentalservice.enums.RentalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "RENTALS", indexes = {
        @Index(name = "IX_RENTALS_ASSET_DATES", columnList = "ASSET_ID, START_DATE, END_DATE"),
        @Index(name = "IX_RENTALS_CUSTOMER", columnList = "CUSTOMER_ID"),
        @Index(name = "IX_RENTALS_MERCHANT", columnList = "MERCHANT_ID")})
public class RentalEntity extends AuditModel {

    @Column(name = "REFERENCE", nullable = false, unique = true, length = 60)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ASSET_ID", nullable = false)
    private AssetEntity asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private SystemUserEntity customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MERCHANT_ID", nullable = false)
    private SystemUserEntity merchant;

    @Column(name = "QUANTITY", nullable = false)
    private int quantity;

    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    @Column(name = "DAYS", nullable = false)
    private int days;

    @Column(name = "DAILY_RATE", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 15)
    private RentalStatus status;
}
