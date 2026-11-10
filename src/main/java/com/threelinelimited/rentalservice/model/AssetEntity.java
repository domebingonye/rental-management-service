package com.threelinelimited.rentalservice.model;

import com.threelinelimited.rentalservice.enums.AssetCategory;
import com.threelinelimited.rentalservice.enums.AssetStatus;
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
@Table(name = "ASSETS", indexes = @Index(name = "IX_ASSETS_MERCHANT", columnList = "MERCHANT_ID"))
public class AssetEntity extends AuditModel {

    @Column(name = "ASSET_CODE", nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MERCHANT_ID", nullable = false)
    private SystemUserEntity merchant;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", nullable = false, length = 20)
    private AssetCategory category;

    @Column(name = "DAILY_RATE", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Column(name = "QUANTITY", nullable = false)
    private int quantity;

    @Column(name = "LOCATION", length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private AssetStatus status;
}
