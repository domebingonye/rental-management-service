package com.threelinelimited.rentalservice.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class PropsReader {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.accessTokenValiditySeconds}")
    private long accessTokenValidityInSeconds;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    @Value("${ignore.urls}")
    private String ignoreUrls;

    @Value("${wallet.currency:NGN}")
    private String walletCurrency;

    @Value("${virtual.account.bank.name:Rental Demo MFB}")
    private String virtualAccountBankName;

    @Value("${virtual.account.bank.code:999}")
    private String virtualAccountBankCode;

    @Value("${virtual.account.name.prefix:RENTAL}")
    private String virtualAccountNamePrefix;

    @Value("${payment.provider.secret-key}")
    private String providerSecretKey;

    @Value("${payment.provider.webhook-secret}")
    private String providerWebhookSecret;

    @Value("${payment.provider.settlement-delay-ms:2000}")
    private long providerSettlementDelayMs;

    @Value("${payment.provider.webhook-max-attempts:5}")
    private int providerWebhookMaxAttempts;

    @Value("${payment.provider.webhook-retry-delay-ms:2000}")
    private long providerWebhookRetryDelayMs;

    @Value("${bootstrap.staff.username}")
    private String bootstrapStaffUsername;

    @Value("${bootstrap.staff.email}")
    private String bootstrapStaffEmail;

    @Value("${bootstrap.staff.password}")
    private String bootstrapStaffPassword;
}
