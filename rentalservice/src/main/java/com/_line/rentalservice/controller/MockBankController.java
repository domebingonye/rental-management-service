package com._line.rentalservice.controller;

import com._line.rentalservice.constant.ApplicationConstants;
import com._line.rentalservice.dto.provider.NameEnquiryResponse;
import com._line.rentalservice.dto.provider.ProviderTransaction;
import com._line.rentalservice.dto.request.BankTransferRequest;
import com._line.rentalservice.service.MockBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * MOCK BANK / PROVIDER SIDE. These endpoints simulate an external system (the customer's commercial
 * bank + the virtual-account provider). They are deliberately outside /api/v1 and do not use the
 * platform's JWT: in real life they would live on someone else's servers.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mock-bank/api/v1")
public class MockBankController {
    private final MockBankService mockBankService;

    /** Step 1 in the customer's bank app: confirm who they are paying. */
    @GetMapping("/name-enquiry")
    public ResponseEntity<NameEnquiryResponse> nameEnquiry(@RequestParam String accountNumber, @RequestParam String bankCode) {
        return ResponseEntity.ok(mockBankService.nameEnquiry(accountNumber, bankCode));
    }

    /** Step 2 in the customer's bank app: send the money. Returns 202 with a PENDING transaction. */
    @PostMapping("/transfers")
    public ResponseEntity<ProviderTransaction> transfer(@Valid @RequestBody BankTransferRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mockBankService.initiateTransfer(request));
    }

    /** Customer's transfer receipt in their bank app. */
    @GetMapping("/transfers/{sessionId}")
    public ResponseEntity<ProviderTransaction> receipt(@PathVariable String sessionId) {
        return ResponseEntity.ok(mockBankService.getTransferReceipt(sessionId));
    }

    /** Provider requery API, called server-to-server by the platform with the provider secret key. */
    @GetMapping("/transactions/{sessionId}")
    public ResponseEntity<ProviderTransaction> requery(
            @RequestHeader(value = ApplicationConstants.AUTHORIZATION_KEY, required = false) String authorization,
            @PathVariable String sessionId) {
        return ResponseEntity.ok(mockBankService.getTransaction(authorization, sessionId));
    }
}
