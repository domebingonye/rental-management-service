package com.threelinelimited.rentalservice.model;

import com.threelinelimited.rentalservice.enums.InboundTransferStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "INBOUND_TRANSFERS")
public class InboundTransferEntity extends AuditModel {

    @Column(name = "SESSION_ID", nullable = false, unique = true, length = 64)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VIRTUAL_ACCOUNT_ID")
    private VirtualAccountEntity virtualAccount;

    @Column(name = "CLAIMED_ACCOUNT_NUMBER", length = 10)
    private String claimedAccountNumber;

    @Column(name = "CLAIMED_AMOUNT", precision = 19, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "VERIFIED_AMOUNT", precision = 19, scale = 2)
    private BigDecimal verifiedAmount;

    @Column(name = "SENDER_NAME", length = 150)
    private String senderName;

    @Column(name = "SENDER_ACCOUNT_NUMBER", length = 10)
    private String senderAccountNumber;

    @Column(name = "SENDER_BANK_NAME", length = 100)
    private String senderBankName;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 25)
    private InboundTransferStatus status;

    @Column(name = "STATUS_REASON")
    private String statusReason;

    @Column(name = "WEBHOOK_COUNT", nullable = false)
    private int webhookCount;

    @Column(name = "VERIFIED_ON")
    private LocalDateTime verifiedOn;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;
}
