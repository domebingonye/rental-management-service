package com.threelinelimited.rentalservice.controller;

import com.threelinelimited.rentalservice.dto.Wallet;
import com.threelinelimited.rentalservice.dto.WalletTransaction;
import com.threelinelimited.rentalservice.dto.request.FundWalletRequest;
import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallet")
public class WalletController {
    private static final String WALLET_HOLDERS =
            "hasAnyAuthority('" + UserRoleType.Code.ROLE_CUSTOMER + "','" + UserRoleType.Code.ROLE_MERCHANT + "')";
    private static final String CUSTOMER_ONLY = "hasAuthority('" + UserRoleType.Code.ROLE_CUSTOMER + "')";

    private final WalletService walletService;

    @GetMapping
    @PreAuthorize(WALLET_HOLDERS)
    public ResponseEntity<Wallet> myWallet(Authentication auth) {
        return ResponseEntity.ok(walletService.getWallet(auth.getName()));
    }

    @GetMapping("/transactions")
    @PreAuthorize(WALLET_HOLDERS)
    public ResponseEntity<Page<WalletTransaction>> myTransactions(Authentication auth,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(walletService.getTransactions(auth.getName(), page, size));
    }

    @PostMapping("/fund")
    @PreAuthorize(CUSTOMER_ONLY)
    public ResponseEntity<WalletTransaction> fund(Authentication auth, @Valid @RequestBody FundWalletRequest request) {
        return ResponseEntity.ok(walletService.fundWallet(auth.getName(), request));
    }
}
