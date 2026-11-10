package com.threelinelimited.rentalservice.service;

import com.threelinelimited.rentalservice.config.PropsReader;
import com.threelinelimited.rentalservice.dto.Wallet;
import com.threelinelimited.rentalservice.dto.WalletTransaction;
import com.threelinelimited.rentalservice.dto.request.FundWalletRequest;
import com.threelinelimited.rentalservice.enums.ResponseCodes;
import com.threelinelimited.rentalservice.enums.TransactionType;
import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.exception.RequestException;
import com.threelinelimited.rentalservice.model.SystemUserEntity;
import com.threelinelimited.rentalservice.model.WalletEntity;
import com.threelinelimited.rentalservice.model.WalletTransactionEntity;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {
    private final WalletDaoService walletDaoService;
    private final WalletTransactionDaoService walletTransactionDaoService;
    private final PropsReader propsReader;

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

    @Transactional
    public WalletTransaction fundWallet(String username, FundWalletRequest request) {
        String reference = StringUtils.hasText(request.getReference())
                ? request.getReference()
                : "FND-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String narration = StringUtils.hasText(request.getNarration()) ? request.getNarration() : "Wallet funding";
        return credit(username, request.getAmount(), reference, narration);
    }

    @Transactional
    public WalletTransaction credit(String username, BigDecimal requestedAmount, String reference, String narration) {
        WalletEntity wallet = walletDaoService.findByUsernameForUpdate(username)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Wallet not found"));

        var existing = walletTransactionDaoService.findByReference(reference);
        if (existing.isPresent()) {
            if (!existing.get().getWallet().getId().equals(wallet.getId())) {
                throw new RequestException(ResponseCodes.CONFLICT, "Reference has already been used");
            }
            log.info("Duplicate credit {} for wallet {} ignored", reference, wallet.getWalletNumber());
            return WalletTransactionDaoService.toDto(existing.get());
        }

        return WalletTransactionDaoService.toDto(postEntry(wallet, TransactionType.CREDIT, requestedAmount, reference, narration));
    }

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

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockInOrder(WalletEntity a, WalletEntity b) {
        WalletEntity first = a.getId() < b.getId() ? a : b;
        WalletEntity second = first == a ? b : a;
        walletDaoService.lockAndRefresh(first);
        walletDaoService.lockAndRefresh(second);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void moveFunds(WalletEntity lockedPayer, WalletEntity lockedPayee, BigDecimal amount, String reference,
                          String debitNarration, String creditNarration) {
        if (lockedPayer.getId().equals(lockedPayee.getId())) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Payer and payee wallets must differ");
        }
        if (!lockedPayer.getCurrency().equals(lockedPayee.getCurrency())) {
            throw new RequestException(ResponseCodes.BAD_REQUEST, "Currency mismatch between wallets");
        }
        postEntry(lockedPayer, TransactionType.DEBIT, amount, reference + "-DR", debitNarration);
        postEntry(lockedPayee, TransactionType.CREDIT, amount, reference + "-CR", creditNarration);
    }

    private WalletEntity findWallet(String username) {
        return walletDaoService.findByUsername(username)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Wallet not found"));
    }
}
