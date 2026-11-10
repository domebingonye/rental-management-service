package com.threelinelimited.rentalservice.service;

import com.threelinelimited.rentalservice.dto.TransferRecipient;
import com.threelinelimited.rentalservice.dto.WalletTransfer;
import com.threelinelimited.rentalservice.dto.request.WalletTransferRequest;
import com.threelinelimited.rentalservice.enums.AccountStatus;
import com.threelinelimited.rentalservice.enums.ResponseCodes;
import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.exception.RequestException;
import com.threelinelimited.rentalservice.model.MerchantProfileEntity;
import com.threelinelimited.rentalservice.model.SystemUserEntity;
import com.threelinelimited.rentalservice.model.WalletEntity;
import com.threelinelimited.rentalservice.model.WalletTransferEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletTransferService {
    private final WalletDaoService walletDaoService;
    private final WalletTransferDaoService walletTransferDaoService;
    private final MerchantProfileDaoService merchantProfileDaoService;
    private final WalletService walletService;

    @Transactional(readOnly = true)
    public TransferRecipient lookupRecipient(String walletNumber) {
        WalletEntity wallet = findPayableMerchantWallet(walletNumber);
        return TransferRecipient.builder()
                .walletNumber(wallet.getWalletNumber())
                .name(merchantName(wallet.getUser()))
                .build();
    }

    @Transactional
    public WalletTransfer transfer(String senderUsername, WalletTransferRequest request) {
        WalletEntity sender = walletDaoService.findByUsername(senderUsername)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Wallet not found"));
        if (sender.getUser().getUserRoleType() != UserRoleType.CUSTOMER) {
            throw new RequestException(ResponseCodes.FORBIDDEN, "Only customers can make transfers");
        }
        WalletEntity recipient = findPayableMerchantWallet(request.getRecipientWalletNumber());
        if (sender.getId().equals(recipient.getId())) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "You cannot transfer to your own wallet");
        }
        if (!sender.getCurrency().equals(recipient.getCurrency())) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Currency mismatch between wallets");
        }

        String reference = StringUtils.hasText(request.getReference())
                ? request.getReference()
                : "TRF-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        walletService.lockInOrder(sender, recipient);

        var existing = walletTransferDaoService.findByReference(reference);
        if (existing.isPresent()) {
            if (!existing.get().getSenderWallet().getId().equals(sender.getId())) {
                throw new RequestException(ResponseCodes.CONFLICT, "Reference has already been used");
            }
            log.info("Duplicate transfer {} from {} ignored", reference, sender.getWalletNumber());
            return WalletTransferDaoService.toDto(existing.get(), senderUsername, sender.getBalance());
        }

        String senderName = sender.getUser().getFirstName() + " " + sender.getUser().getSurname();
        String recipientName = merchantName(recipient.getUser());
        String note = StringUtils.hasText(request.getNarration()) ? " - " + request.getNarration().trim() : "";

        walletService.moveFunds(sender, recipient, request.getAmount(), reference,
                "Payment to " + recipientName + note, "Payment from " + senderName + note);

        WalletTransferEntity transfer = walletTransferDaoService.save(new WalletTransferEntity()
                .setReference(reference)
                .setSenderWallet(sender)
                .setRecipientWallet(recipient)
                .setAmount(request.getAmount())
                .setCurrency(sender.getCurrency())
                .setSenderName(senderName)
                .setRecipientName(recipientName)
                .setNarration(StringUtils.hasText(request.getNarration()) ? request.getNarration().trim() : null));

        log.info("Transfer {}: {} {} from wallet {} to merchant wallet {}", reference, request.getAmount(),
                sender.getCurrency(), sender.getWalletNumber(), recipient.getWalletNumber());
        return WalletTransferDaoService.toDto(transfer, senderUsername, sender.getBalance());
    }

    @Transactional(readOnly = true)
    public Page<WalletTransfer> getTransfers(String username, int page, int size) {
        return walletTransferDaoService.findInvolving(username, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100)))
                .map(t -> WalletTransferDaoService.toDto(t, username, null));
    }

    @Transactional(readOnly = true)
    public WalletTransfer getTransfer(String username, String reference) {
        return walletTransferDaoService.findByReference(reference)
                .filter(t -> t.getSenderWallet().getUser().getUsername().equals(username)
                        || t.getRecipientWallet().getUser().getUsername().equals(username))
                .map(t -> WalletTransferDaoService.toDto(t, username, null))
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Transfer not found"));
    }

    private WalletEntity findPayableMerchantWallet(String walletNumber) {
        return walletDaoService.findByWalletNumber(walletNumber)
                .filter(w -> w.getUser().getUserRoleType() == UserRoleType.MERCHANT)
                .filter(w -> w.getUser().getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Merchant wallet not found"));
    }

    private String merchantName(SystemUserEntity merchant) {
        return merchantProfileDaoService.findByUserCode(merchant.getCode())
                .map(MerchantProfileEntity::getBusinessName)
                .orElse(merchant.getFirstName() + " " + merchant.getSurname());
    }
}
