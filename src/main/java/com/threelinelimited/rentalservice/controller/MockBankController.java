package com.threelinelimited.rentalservice.controller;

import com.threelinelimited.rentalservice.constant.ApplicationConstants;
import com.threelinelimited.rentalservice.dto.provider.NameEnquiryResponse;
import com.threelinelimited.rentalservice.dto.provider.ProviderTransaction;
import com.threelinelimited.rentalservice.dto.request.BankTransferRequest;
import com.threelinelimited.rentalservice.service.MockBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mock-bank/api/v1")
public class MockBankController {
    private final MockBankService mockBankService;

    @GetMapping("/name-enquiry")
    public ResponseEntity<NameEnquiryResponse> nameEnquiry(@RequestParam String accountNumber, @RequestParam String bankCode) {
        return ResponseEntity.ok(mockBankService.nameEnquiry(accountNumber, bankCode));
    }

    @PostMapping("/transfers")
    public ResponseEntity<ProviderTransaction> transfer(@Valid @RequestBody BankTransferRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mockBankService.initiateTransfer(request));
    }

    @GetMapping("/transfers/{sessionId}")
    public ResponseEntity<ProviderTransaction> receipt(@PathVariable String sessionId) {
        return ResponseEntity.ok(mockBankService.getTransferReceipt(sessionId));
    }

    @GetMapping("/transactions/{sessionId}")
    public ResponseEntity<ProviderTransaction> requery(
            @RequestHeader(value = ApplicationConstants.AUTHORIZATION_KEY, required = false) String authorization,
            @PathVariable String sessionId) {
        return ResponseEntity.ok(mockBankService.getTransaction(authorization, sessionId));
    }
}
