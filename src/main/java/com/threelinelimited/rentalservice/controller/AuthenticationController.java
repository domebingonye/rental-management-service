package com.threelinelimited.rentalservice.controller;

import com.threelinelimited.rentalservice.constant.ApplicationConstants;
import com.threelinelimited.rentalservice.dto.SystemUser;
import com.threelinelimited.rentalservice.dto.request.LoginRequest;
import com.threelinelimited.rentalservice.dto.request.MerchantSignupRequest;
import com.threelinelimited.rentalservice.dto.request.SignupRequest;
import com.threelinelimited.rentalservice.dto.response.LoginResponse;
import com.threelinelimited.rentalservice.enums.AccountStatus;
import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.service.AuthenticationService;
import com.threelinelimited.rentalservice.service.SystemUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/authentication")
public class AuthenticationController {
    private static final String STAFF_ONLY = "hasAuthority('" + UserRoleType.Code.ROLE_STAFF + "')";

    private final AuthenticationService authenticationService;
    private final SystemUserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(HttpServletRequest request, @Valid @RequestBody LoginRequest login) {
        return ResponseEntity.ok(authenticationService.processLogin(request, login));
    }

    @PostMapping("/signup/customer")
    public ResponseEntity<SystemUser> signupCustomer(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.onboardCustomer(request));
    }

    @PostMapping("/signup/merchant")
    public ResponseEntity<SystemUser> signupMerchant(@Valid @RequestBody MerchantSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.onboardMerchant(request));
    }

    @GetMapping("/user")
    public ResponseEntity<LoginResponse> getUser(
            @RequestHeader(value = ApplicationConstants.AUTHORIZATION_KEY, required = false) String authorization) {
        return ResponseEntity.ok(authenticationService.getUser(authorization));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authenticationService.logout();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/staff")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<SystemUser> createStaff(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.onboardStaff(request));
    }

    @GetMapping("/merchants/pending")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<List<SystemUser>> pendingMerchants() {
        return ResponseEntity.ok(userService.findPendingMerchants());
    }

    @PutMapping("/merchants/{code}/approve")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<SystemUser> approveMerchant(@PathVariable String code, Authentication auth) {
        return ResponseEntity.ok(userService.approveMerchant(code, auth.getName()));
    }

    @PutMapping("/user/{code}/status")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<SystemUser> updateStatus(@PathVariable String code, @RequestParam AccountStatus status,
                                                   Authentication auth) {
        return ResponseEntity.ok(userService.updateStatus(code, status, auth.getName()));
    }
}
