package com._line.rentalservice.model;

import com._line.rentalservice.enums.BankTransferStatus;
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
@Table(name = "MOCK_BANK_TRANSFERS")
public class MockBankTransferEntity extends AuditModel {

    @Column(name = "SESSION_ID", nullable = false, unique = true, length = 30)
    private String sessionId;

    @Column(name = "SOURCE_BANK_NAME", nullable = false, length = 100)
    private String sourceBankName;

    @Column(name = "SOURCE_ACCOUNT_NUMBER", nullable = false, length = 10)
    private String sourceAccountNumber;

    @Column(name = "SOURCE_ACCOUNT_NAME", nullable = false, length = 150)
    private String sourceAccountName;

    @Column(name = "DESTINATION_ACCOUNT_NUMBER", nullable = false, length = 10)
    private String destinationAccountNumber;

    @Column(name = "DESTINATION_ACCOUNT_NAME", nullable = false, length = 150)
    private String destinationAccountName;

    @Column(name = "DESTINATION_BANK_CODE", nullable = false, length = 3)
    private String destinationBankCode;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Column(name = "NARRATION")
    private String narration;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 15)
    private BankTransferStatus status;

    @Column(name = "FAILURE_REASON")
    private String failureReason;

    @Column(name = "COMPLETED_ON")
    private LocalDateTime completedOn;

    @Column(name = "WEBHOOK_ATTEMPTS", nullable = false)
    private int webhookAttempts;

    @Column(name = "WEBHOOK_DELIVERED", nullable = false)
    private boolean webhookDelivered;
}
