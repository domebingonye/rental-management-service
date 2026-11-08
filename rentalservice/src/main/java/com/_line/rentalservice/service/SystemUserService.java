package com._line.rentalservice.service;

import com._line.rentalservice.dto.SystemUser;
import com._line.rentalservice.dto.request.MerchantSignupRequest;
import com._line.rentalservice.dto.request.SignupRequest;
import com._line.rentalservice.enums.AccountStatus;
import com._line.rentalservice.enums.ResponseCodes;
import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.exception.RequestException;
import com._line.rentalservice.model.MerchantProfileEntity;
import com._line.rentalservice.model.SystemUserEntity;
import com._line.rentalservice.model.VirtualAccountEntity;
import com._line.rentalservice.model.WalletEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Onboarding (customer / merchant / staff) and the {@link UserDetailsService} used by Spring Security.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemUserService implements UserDetailsService {
    private final SystemUserDaoService systemUserDaoService;
    private final MerchantProfileDaoService merchantProfileDaoService;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final VirtualAccountService virtualAccountService;

    // ---------------------------------------------------------------- Spring Security

    /** Accepts either username or email. Throws UsernameNotFoundException so the provider reports "bad credentials". */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) {
        SystemUserEntity user = systemUserDaoService.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(user.getUserRoleType().name())))
                .disabled(user.getStatus() == AccountStatus.PENDING_APPROVAL)
                .accountLocked(user.getStatus() == AccountStatus.SUSPENDED)
                .build();
    }

    // ---------------------------------------------------------------- Onboarding

    /** Self sign-up. Customers can log in straight away. */
    @Transactional
    public SystemUser onboardCustomer(SignupRequest request) {
        SystemUserEntity user = createUser(request, UserRoleType.CUSTOMER, AccountStatus.ACTIVE);
        WalletEntity wallet = walletService.createWallet(user);
        VirtualAccountEntity virtualAccount = virtualAccountService.createForWallet(wallet, user.getFirstName() + " " + user.getSurname());
        log.info("Onboarded customer {}", user.getCode());
        return withWallet(SystemUserDaoService.toDto(user), wallet, virtualAccount);
    }

    /** Self sign-up. Merchants list items for rent, so they wait for staff approval before they can log in. */
    @Transactional
    public SystemUser onboardMerchant(MerchantSignupRequest request) {
        SystemUserEntity user = createUser(request, UserRoleType.MERCHANT, AccountStatus.PENDING_APPROVAL);
        MerchantProfileEntity profile = merchantProfileDaoService.save(new MerchantProfileEntity()
                .setUser(user)
                .setBusinessName(request.getBusinessName().trim())
                .setBusinessAddress(request.getBusinessAddress())
                .setRegistrationNumber(request.getRegistrationNumber()));
        // Created now (not on approval) so the merchant's wallet exists the moment they can trade.
        WalletEntity wallet = walletService.createWallet(user);
        VirtualAccountEntity virtualAccount = virtualAccountService.createForWallet(wallet, profile.getBusinessName());
        log.info("Onboarded merchant {} (pending approval)", user.getCode());
        return withWallet(SystemUserDaoService.toDto(user, profile), wallet, virtualAccount);
    }

    private static SystemUser withWallet(SystemUser dto, WalletEntity wallet, VirtualAccountEntity virtualAccount) {
        dto.setWallet(WalletDaoService.toDto(wallet));
        dto.setVirtualAccount(VirtualAccountDaoService.toDto(virtualAccount));
        return dto;
    }

    /** Staff cannot self-register; an existing staff member creates them (or the startup bootstrap does). */
    @Transactional
    public SystemUser onboardStaff(SignupRequest request) {
        SystemUserEntity user = createUser(request, UserRoleType.STAFF, AccountStatus.ACTIVE);
        log.info("Onboarded staff {}", user.getCode());
        return SystemUserDaoService.toDto(user);
    }

    // ---------------------------------------------------------------- Staff operations

    @Transactional(readOnly = true)
    public List<SystemUser> findPendingMerchants() {
        return merchantProfileDaoService.findByUserStatus(AccountStatus.PENDING_APPROVAL).stream()
                .map(p -> SystemUserDaoService.toDto(p.getUser(), p))
                .toList();
    }

    @Transactional
    public SystemUser approveMerchant(String merchantCode, String approvedBy) {
        MerchantProfileEntity profile = merchantProfileDaoService.findByUserCode(merchantCode)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Merchant not found"));
        SystemUserEntity user = profile.getUser();
        if (user.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new RequestException(ResponseCodes.CONFLICT, "Merchant is not pending approval");
        }
        user.setStatus(AccountStatus.ACTIVE);
        profile.setApprovedOn(LocalDateTime.now()).setApprovedBy(approvedBy);
        log.info("Merchant {} approved by {}", merchantCode, approvedBy);
        return SystemUserDaoService.toDto(user, profile);
    }

    /** Suspend or reactivate any account. Staff cannot suspend themselves (avoids locking out the last admin). */
    @Transactional
    public SystemUser updateStatus(String code, AccountStatus status, String actingUsername) {
        if (status == AccountStatus.PENDING_APPROVAL) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Status must be ACTIVE or SUSPENDED");
        }
        SystemUserEntity user = systemUserDaoService.findByCode(code)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "User not found"));
        if (user.getUsername().equals(actingUsername)) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "You cannot change your own status");
        }
        if (user.getStatus() == AccountStatus.PENDING_APPROVAL && status == AccountStatus.ACTIVE) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Use the approve endpoint for pending merchants");
        }
        user.setStatus(status);
        log.info("User {} set to {} by {}", code, status, actingUsername);
        return SystemUserDaoService.toDto(user);
    }

    // ---------------------------------------------------------------- internals

    private SystemUserEntity createUser(SignupRequest request, UserRoleType role, AccountStatus status) {
        if (systemUserDaoService.usernameExists(request.getUsername())) {
            throw new RequestException(ResponseCodes.CONFLICT, "The username " + request.getUsername() + " exists");
        }
        if (systemUserDaoService.emailExists(request.getEmail())) {
            throw new RequestException(ResponseCodes.CONFLICT, "The email " + request.getEmail() + " exists");
        }
        // A username that looks like someone else's email would make username-or-email login ambiguous.
        if (request.getUsername().contains("@")) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Username cannot contain '@'");
        }
        return systemUserDaoService.save(new SystemUserEntity()
                .setEmail(SystemUserDaoService.normalize(request.getEmail()))
                .setUsername(SystemUserDaoService.normalize(request.getUsername()))
                .setPassword(passwordEncoder.encode(request.getPassword()))
                .setFirstName(request.getFirstName().trim())
                .setSurname(request.getSurname().trim())
                .setPhoneNumber(request.getPhoneNumber())
                .setCode(systemUserDaoService.generateUserCode(role))
                .setUserRoleType(role)
                .setStatus(status));
    }
}
