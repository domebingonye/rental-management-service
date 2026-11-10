package com.threelinelimited.rentalservice.controller;

import com.threelinelimited.rentalservice.constant.ApplicationConstants;
import com.threelinelimited.rentalservice.dto.InboundTransfer;
import com.threelinelimited.rentalservice.service.InboundTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhooks")
public class WebhookController {
    private final InboundTransferService inboundTransferService;

    @PostMapping("/bank-transfer")
    public ResponseEntity<InboundTransfer> bankTransfer(
            @RequestBody String rawBody,
            @RequestHeader(value = ApplicationConstants.PROVIDER_SIGNATURE_HEADER, required = false) String signature) {
        return ResponseEntity.ok(inboundTransferService.handleWebhook(rawBody, signature));
    }
}
