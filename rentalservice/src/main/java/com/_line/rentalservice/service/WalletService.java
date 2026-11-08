package com._line.rentalservice.service;

import com._line.rentalservice.config.PropsReader;
import com._line.rentalservice.dto.Wallet;
import com._line.rentalservice.dto.WalletTransaction;
import com._line.rentalservice.dto.request.FundWalletRequest;
import com._line.rentalservice.enums.ResponseCodes;
import com._line.rentalservice.enums.TransactionType;
import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.exception.RequestException;
import com._line.rentalservice.model.SystemUserEntity;
import com._line.rentalservice.model.WalletEntity;
import com._line.rentalservice.model.WalletTransactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/** Wallet business rules. All persistence goes through {@link WalletDaoService} / {@link WalletTransactionDaoService}. */
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {
    private final WalletDaoService walletDaoService;
    private final WalletTransactionDaoService walletTransactionDaoService;
    private final PropsReader propsReader;

    /**
     * Called from onboarding. MANDATORY: it must join the sign-up transaction, so if creating the wallet
     * fails the account is rolled back too — there is never a customer or merchant without a wallet.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public WalletEntity createWallet(SystemUserEntity user) {
        if (user.getUserRoleType() == UserRoleType.STAFF) {
            throw new IllegalArgumentException("Staff accounts do not have wallets");
        }
        WalletEntity wallet = walletDaoService.save(new WalletEntity()
                .setUser(user)
                .setWalletNumber(walletDaoService.generateWalletNumber())
                .setBalance(BigDecimal.ZERO.setScale(2))
                .setCurrency(propsReader.getWalletCurrency()));
        log.info("Created wallet {} for {} {}", wallet.getWalletNumber(), user.getUserRoleType(), user.getCode());
        return wallet;
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(String username) {
        return WalletDaoService.toDto(findWallet(username));
    }

    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactions(String username, int page, int size) {
        WalletEntity wallet = findWallet(username);
        return walletTransactionDaoService
                .findByWalletId(wallet.getId(), PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100)))
                .map(WalletTransactionDaoService::toDto);
    }

    /**
     * Credits the caller's wallet.
     * <p>
     * In production this is triggered by a verified payment-gateway webhook (e.g. Paystack/Flutterwave),
     * not directly by the customer; here the gateway step is simulated so the ledger logic can be shown.
     */
    @Transactional
    public WalletTransaction fundWallet(String username, FundWalletRequest request) {
        String reference = StringUtils.hasText(request.getReference())
                ? request.getReference()
                : "FND-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String narration = StringUtils.hasText(request.getNarration()) ? request.getNarration() : "Wallet funding";
        return credit(username, request.getAmount(), reference, narration);
    }

    /**
     * The single place a wallet is credited (card funding, virtual-account transfers, …).
     * Idempotent on {@code reference}: the same reference for the same wallet returns the original entry.
     */
    @Transactional
    public WalletTransaction credit(String username, BigDecimal requestedAmount, String reference, String narration) {
        // 1. Lock the wallet row first. Every balance change for this wallet now queues behind us.
        WalletEntity wallet = walletDaoService.findByUsernameForUpdate(username)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Wallet not found"));

        // 2. Idempotency, checked under the lock so two identical retries cannot both pass.
        var existing = walletTransactionDaoService.findByReference(reference);
        if (existing.isPresent()) {
            if (!existing.get().getWallet().getId().equals(wallet.getId())) {
                throw new RequestException(ResponseCodes.CONFLICT, "Reference has already been used");
            }
            log.info("Duplicate credit {} for wallet {} ignored", reference, wallet.getWalletNumber());
            return WalletTransactionDaoService.toDto(existing.get());
        }

        // 3. Apply the credit and write the ledger entry in the same transaction.
        return WalletTransactionDaoService.toDto(postEntry(wallet, TransactionType.CREDIT, requestedAmount, reference, narration));
    }

    /**
     * Applies one ledger entry to a wallet the caller has ALREADY LOCKED, in the caller's transaction.
     * This is the only code that changes a balance. A debit never takes a wallet below zero.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public WalletTransactionEntity postEntry(WalletEntity lockedWallet, TransactionType type, BigDecimal requestedAmount,
                                            String reference, String narration) {
        BigDecimal amount = requestedAmount.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal before = lockedWallet.getBalance();
        BigDecimal after = type == TransactionType.CREDIT ? before.add(amount) : before.subtract(amount);
        if (after.signum() < 0) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Insufficient wallet balance");
        }
        lockedWallet.setBalance(after);

        WalletTransactionEntity tx = walletTransactionDaoService.save(new WalletTransactionEntity()
                .setWallet(lockedWallet)
                .setReference(reference)
                .setType(type)
                .setAmount(amount)
                .setBalanceBefore(before)
                .setBalanceAfter(after)
                .setNarration(narration));

        log.info("Wallet {} {} {} {} (ref {}), balance {} -> {}", lockedWallet.getWalletNumber(),
                type == TransactionType.CREDIT ? "credited with" : "debited", amount, lockedWallet.getCurrency(),
                reference, before, after);
        return tx;
    }

    private WalletEntity findWallet(String username) {
        return walletDaoService.findByUsername(username)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Wallet not found"));
    }
}
