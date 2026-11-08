package com._line.rentalservice.service;

import com._line.rentalservice.dto.SystemUser;
import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.model.MerchantProfileEntity;
import com._line.rentalservice.model.SystemUserEntity;
import com._line.rentalservice.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;

/** Persistence-facing helpers for SYS_USERS: lookups, code generation and entity → DTO mapping. */
@Service
@RequiredArgsConstructor
public class SystemUserDaoService {
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SystemUserRepository systemUserRepository;

    public static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public Optional<SystemUserEntity> findByUsernameOrEmail(String usernameOrEmail) {
        String key = normalize(usernameOrEmail);
        return systemUserRepository.findByUsernameOrEmail(key, key);
    }

    public Optional<SystemUserEntity> findByUsername(String username) {
        return systemUserRepository.findByUsername(normalize(username));
    }

    public Optional<SystemUserEntity> findByCode(String code) {
        return systemUserRepository.findByCode(code);
    }

    public boolean usernameExists(String username) {
        return systemUserRepository.existsByUsername(normalize(username));
    }

    public boolean emailExists(String email) {
        return systemUserRepository.existsByEmail(normalize(email));
    }

    public boolean roleExists(UserRoleType role) {
        return systemUserRepository.existsByUserRoleType(role);
    }

    public SystemUserEntity save(SystemUserEntity entity) {
        return systemUserRepository.save(entity);
    }

    /** e.g. CUS-8F3K2A9Q, MER-…, STF-… — unguessable, so it is safe to expose in URLs. */
    public String generateUserCode(UserRoleType role) {
        String prefix = switch (role) {
            case CUSTOMER -> "CUS";
            case MERCHANT -> "MER";
            case STAFF -> "STF";
        };
        String code;
        do {
            StringBuilder sb = new StringBuilder(prefix).append('-');
            for (int i = 0; i < 8; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            code = sb.toString();
        } while (systemUserRepository.existsByCode(code));
        return code;
    }

    public static SystemUser toDto(SystemUserEntity e) {
        return toDto(e, null);
    }

    public static SystemUser toDto(SystemUserEntity e, MerchantProfileEntity profile) {
        return SystemUser.builder()
                .code(e.getCode())
                .email(e.getEmail())
                .username(e.getUsername())
                .firstName(e.getFirstName())
                .surname(e.getSurname())
                .phoneNumber(e.getPhoneNumber())
                .userRoleType(e.getUserRoleType())
                .status(e.getStatus())
                .merchant(profile == null ? null : SystemUser.MerchantDetails.builder()
                        .businessName(profile.getBusinessName())
                        .businessAddress(profile.getBusinessAddress())
                        .registrationNumber(profile.getRegistrationNumber())
                        .approvedBy(profile.getApprovedBy())
                        .build())
                .build();
    }
}
