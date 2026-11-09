package com._line.rentalservice.controller;

import com._line.rentalservice.dto.TransferRecipient;
import com._line.rentalservice.dto.WalletTransfer;
import com._line.rentalservice.dto.request.WalletTransferRequest;
import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.service.WalletTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallet/transfers")
public class WalletTransferController {
    private static final String CUSTOMER_ONLY = "hasAuthority('" + UserRoleType.Code.ROLE_CUSTOMER + "')";
    private static final String WALLET_HOLDERS =
            "hasAnyAuthority('" + UserRoleType.Code.ROLE_CUSTOMER + "','" + UserRoleType.Code.ROLE_MERCHANT + "')";

    private final WalletTransferService walletTransferService;

    @GetMapping("/recipient")
    @PreAuthorize(CUSTOMER_ONLY)
    public ResponseEntity<TransferRecipient> lookupRecipient(@RequestParam String walletNumber) {
        return ResponseEntity.ok(walletTransferService.lookupRecipient(walletNumber));
    }

    @PostMapping
    @PreAuthorize(CUSTOMER_ONLY)
    public ResponseEntity<WalletTransfer> transfer(Authentication auth, @Valid @RequestBody WalletTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletTransferService.transfer(auth.getName(), request));
    }

    @GetMapping
    @PreAuthorize(WALLET_HOLDERS)
    public ResponseEntity<Page<WalletTransfer>> myTransfers(Authentication auth,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(walletTransferService.getTransfers(auth.getName(), page, size));
    }

    @GetMapping("/{reference}")
    @PreAuthorize(WALLET_HOLDERS)
    public ResponseEntity<WalletTransfer> getTransfer(Authentication auth, @PathVariable String reference) {
        return ResponseEntity.ok(walletTransferService.getTransfer(auth.getName(), reference));
    }
}
