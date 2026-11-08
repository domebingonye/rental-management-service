package com._line.rentalservice.model;

import com._line.rentalservice.enums.AccountStatus;
import com._line.rentalservice.enums.UserRoleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Every login-capable account (customer, merchant, staff) lives in one table, discriminated by role.
 * Role-specific data lives in its own table (e.g. {@link MerchantProfileEntity}).
 */
@Entity
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "SYS_USERS")
public class SystemUserEntity extends AuditModel {

    /** Stored lower-cased so lookups are case-insensitive on every database. */
    @Column(name = "EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "PASSWORD", nullable = false, length = 100)
    private String password;

    @Column(name = "FIRST_NAME", nullable = false, length = 80)
    private String firstName;

    @Column(name = "SURNAME", nullable = false, length = 80)
    private String surname;

    @Column(name = "PHONE_NUMBER", length = 20)
    private String phoneNumber;

    /** Public, non-sequential identifier (e.g. CUS-8F3K2A9Q) used in URLs instead of the DB id. */
    @Column(name = "USER_CODE", nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private UserRoleType userRoleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "LAST_LOGIN_ON")
    private LocalDateTime lastLoginOn;
}
