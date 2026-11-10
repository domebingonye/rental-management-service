package com.threelinelimited.rentalservice.service;

import com.threelinelimited.rentalservice.dto.VirtualAccount;
import com.threelinelimited.rentalservice.model.VirtualAccountEntity;
import com.threelinelimited.rentalservice.repository.VirtualAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VirtualAccountDaoService {
    private static final int[] NUBAN_WEIGHTS = {3, 7, 3, 3, 7, 3, 3, 7, 3, 3, 7, 3};
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VirtualAccountRepository virtualAccountRepository;

    public VirtualAccountEntity save(VirtualAccountEntity entity) {
        return virtualAccountRepository.save(entity);
    }

    public Optional<VirtualAccountEntity> findByUsername(String username) {
        return virtualAccountRepository.findByWalletUserUsername(username);
    }

    public Optional<VirtualAccountEntity> findByAccountNumber(String accountNumber) {
        return virtualAccountRepository.findByAccountNumber(accountNumber);
    }

    public String generateAccountNumber(String bankCode) {
        String number;
        do {
            String serial = String.valueOf(100_000_000 + RANDOM.nextInt(900_000_000));
            number = serial + nubanCheckDigit(bankCode, serial);
        } while (virtualAccountRepository.existsByAccountNumber(number));
        return number;
    }

    static int nubanCheckDigit(String bankCode, String serial) {
        String digits = bankCode + serial;
        if (!digits.matches("\\d{12}")) {
            throw new IllegalArgumentException("Bank code must be 3 digits and serial 9 digits");
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (digits.charAt(i) - '0') * NUBAN_WEIGHTS[i];
        return (10 - sum % 10) % 10;
    }

    public static boolean isValidNuban(String bankCode, String accountNumber) {
        if (accountNumber == null || !accountNumber.matches("\\d{10}")) return false;
        return nubanCheckDigit(bankCode, accountNumber.substring(0, 9)) == accountNumber.charAt(9) - '0';
    }

    public static VirtualAccount toDto(VirtualAccountEntity v) {
        return VirtualAccount.builder()
                .accountNumber(v.getAccountNumber())
                .accountName(v.getAccountName())
                .bankName(v.getBankName())
                .bankCode(v.getBankCode())
                .walletNumber(v.getWallet().getWalletNumber())
                .build();
    }
}
