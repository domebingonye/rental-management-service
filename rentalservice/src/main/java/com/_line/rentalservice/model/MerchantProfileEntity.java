package com._line.rentalservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Business details captured when a merchant onboards. */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "MERCHANT_PROFILES")
public class MerchantProfileEntity extends AuditModel {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private SystemUserEntity user;

    @Column(name = "BUSINESS_NAME", nullable = false, length = 150)
    private String businessName;

    @Column(name = "BUSINESS_ADDRESS")
    private String businessAddress;

    /** Company registration number (e.g. CAC RC number). */
    @Column(name = "REGISTRATION_NUMBER", length = 50)
    private String registrationNumber;

    @Column(name = "APPROVED_ON")
    private LocalDateTime approvedOn;

    /** Username of the staff member who approved this merchant. */
    @Column(name = "APPROVED_BY", length = 80)
    private String approvedBy;
}
