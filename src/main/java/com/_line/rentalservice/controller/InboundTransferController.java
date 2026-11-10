package com._line.rentalservice.controller;

import com._line.rentalservice.dto.InboundTransfer;
import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.service.InboundTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inbound-transfers")
public class InboundTransferController {
    private static final String WALLET_HOLDERS =
            "hasAnyAuthority('" + UserRoleType.Code.ROLE_CUSTOMER + "','" + UserRoleType.Code.ROLE_MERCHANT + "')";
    private static final String STAFF_ONLY = "hasAuthority('" + UserRoleType.Code.ROLE_STAFF + "')";

    private final InboundTransferService inboundTransferService;

    @GetMapping
    @PreAuthorize(WALLET_HOLDERS)
    public ResponseEntity<Page<InboundTransfer>> myTransfers(Authentication auth,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inboundTransferService.getMyTransfers(auth.getName(), page, size));
    }

    @PostMapping("/{sessionId}/requery")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<InboundTransfer> requery(@PathVariable String sessionId) {
        return ResponseEntity.ok(inboundTransferService.verify(sessionId));
    }
}
