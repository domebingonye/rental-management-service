package com.threelinelimited.rentalservice.service;

import com.threelinelimited.rentalservice.config.PropsReader;
import com.threelinelimited.rentalservice.dto.VirtualAccount;
import com.threelinelimited.rentalservice.enums.ResponseCodes;
import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.exception.RequestException;
import com.threelinelimited.rentalservice.model.MerchantProfileEntity;
import com.threelinelimited.rentalservice.model.SystemUserEntity;
import com.threelinelimited.rentalservice.model.VirtualAccountEntity;
import com.threelinelimited.rentalservice.model.WalletEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class VirtualAccountService {
    private final VirtualAccountDaoService virtualAccountDaoService;
    private final WalletDaoService walletDaoService;
    private final MerchantProfileDaoService merchantProfileDaoService;
    private final PropsReader propsReader;

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

    @Transactional
    public VirtualAccount generateVirtualAccount(String username) {
        var existing = virtualAccountDaoService.findByUsername(username);
        if (existing.isPresent()) return VirtualAccountDaoService.toDto(existing.get());

        WalletEntity wallet = walletDaoService.findByUsernameForUpdate(username)
                .orElseThrow(() -> new RequestException(ResponseCodes.NOT_FOUND, "Wallet not found"));
        existing = virtualAccountDaoService.findByUsername(username);
        if (existing.isPresent()) return VirtualAccountDaoService.toDto(existing.get());

        return VirtualAccountDaoService.toDto(createForWallet(wallet, holderName(wallet.getUser())));
    }

    private String holderName(SystemUserEntity user) {
        if (user.getUserRoleType() == UserRoleType.MERCHANT) {
            return merchantProfileDaoService.findByUserCode(user.getCode())
                    .map(MerchantProfileEntity::getBusinessName)
                    .orElse(user.getFirstName() + " " + user.getSurname());
        }
        return user.getFirstName() + " " + user.getSurname();
    }

    private String accountName(String holderName) {
        String name = (propsReader.getVirtualAccountNamePrefix() + "/" + holderName.trim()).toUpperCase(Locale.ROOT);
        return name.length() > 150 ? name.substring(0, 150) : name;
    }
}
