package com._line.rentalservice.service;

import com._line.rentalservice.config.PropsReader;
import com._line.rentalservice.dto.VirtualAccount;
import com._line.rentalservice.enums.ResponseCodes;
import com._line.rentalservice.enums.UserRoleType;
import com._line.rentalservice.exception.RequestException;
import com._line.rentalservice.model.MerchantProfileEntity;
import com._line.rentalservice.model.SystemUserEntity;
import com._line.rentalservice.model.VirtualAccountEntity;
import com._line.rentalservice.model.WalletEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Virtual accounts: one per wallet. There is no payment provider, so account numbers are generated
 * in-house. Transfers into them arrive through the provider webhook (see InboundTransferService).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VirtualAccountService {
    private final VirtualAccountDaoService virtualAccountDaoService;
    private final WalletDaoService walletDaoService;
    private final MerchantProfileDaoService merchantProfileDaoService;
    private final PropsReader propsReader;

    /**
     * Called from onboarding right after the wallet is created. MANDATORY: joins the sign-up transaction,
     * so account, wallet and virtual account are created together or not at all.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public VirtualAccountEntity createForWallet(WalletEntity wallet, String holderName) {
        String bankCode = propsReader.getVirtualAccountBankCode();
        VirtualAccountEntity account = virtualAccountDaoService.save(new VirtualAccountEntity()
                .setWallet(wallet)
                .setAccountNumber(virtualAccountDaoService.generateAccountNumber(bankCode))
                .setAccountName(accountName(holderName))
                .setBankName(propsReader.getVirtualAccountBankName())
                .setBankCode(bankCode));
        log.info("Created virtual account {} for wallet {}", account.getAccountNumber(), wallet.getWalletNumber());
        return account;
    }

    @Transactional(readOnly = true)
    public VirtualAccount getVirtualAccount(String username) {
        return virtualAccountDaoService.findByUsername(username)
                .map(VirtualAccountDaoService::toDto)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Virtual account not found"));
    }

    /**
     * Returns the caller's virtual account, creating it first if they do not have one
     * (covers accounts onboarded before virtual accounts existed). Safe to call repeatedly.
     */
    @Transactional
    public VirtualAccount generateVirtualAccount(String username) {
        var existing = virtualAccountDaoService.findByUsername(username);
        if (existing.isPresent()) return VirtualAccountDaoService.toDto(existing.get());

        // Lock the wallet so two simultaneous "generate" calls cannot both create one.
        WalletEntity wallet = walletDaoService.findByUsernameForUpdate(username)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Wallet not found"));
        existing = virtualAccountDaoService.findByUsername(username);
        if (existing.isPresent()) return VirtualAccountDaoService.toDto(existing.get());

        return VirtualAccountDaoService.toDto(createForWallet(wallet, holderName(wallet.getUser())));
    }

    // ---------------------------------------------------------------- internals

    /** Merchants are named after their business, customers after themselves. */
    private String holderName(SystemUserEntity user) {
        if (user.getUserRoleType() == UserRoleType.MERCHANT) {
            return merchantProfileDaoService.findByUserCode(user.getCode())
                    .map(MerchantProfileEntity::getBusinessName)
                    .orElse(user.getFirstName() + " " + user.getSurname());
        }
        return user.getFirstName() + " " + user.getSurname();
    }

    /** e.g. "RENTAL/ADA OBI" — the prefix tells a sender the money is going to the platform. */
    private String accountName(String holderName) {
        String name = (propsReader.getVirtualAccountNamePrefix() + "/" + holderName.trim()).toUpperCase(Locale.ROOT);
        return name.length() > 150 ? name.substring(0, 150) : name;
    }
}
