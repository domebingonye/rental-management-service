package com._line.rentalservice.controller;

import com._line.rentalservice.dto.VirtualAccount;
import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.service.VirtualAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/virtual-account")
public class VirtualAccountController {
    private static final String WALLET_HOLDERS =
            "hasAnyAuthority('" + UserRoleType.Code.ROLE_CUSTOMER + "','" + UserRoleType.Code.ROLE_MERCHANT + "')";

    private final VirtualAccountService virtualAccountService;

    /** The caller's virtual account (account number to share for transfers into their wallet). */
    @GetMapping
    @PreAuthorize(WALLET_HOLDERS)
    public ResponseEntity<VirtualAccount> myVirtualAccount(Authentication auth) {
        return ResponseEntity.ok(virtualAccountService.getVirtualAccount(auth.getName()));
    }

    /** Creates the caller's virtual account if missing, otherwise returns the existing one. */
    @PostMapping
    @PreAuthorize(WALLET_HOLDERS)
    public ResponseEntity<VirtualAccount> generate(Authentication auth) {
        return ResponseEntity.ok(virtualAccountService.generateVirtualAccount(auth.getName()));
    }
}
