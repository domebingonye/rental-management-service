package com._line.rentalservice.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class PropsReader {
    /** Base64-encoded HMAC key, at least 256 bits. */
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.accessTokenValiditySeconds}")
    private long accessTokenValidityInSeconds;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    /** Comma-separated Ant patterns the JWT filter skips. */
    @Value("${ignore.urls}")
    private String ignoreUrls;

    /** ISO 4217 currency for new wallets. */
    @Value("${wallet.currency:NGN}")
    private String walletCurrency;

    @Value("${virtual.account.bank.name:Rental Demo MFB}")
    private String virtualAccountBankName;

    /** 3-digit code used in the NUBAN check-digit calculation. */
    @Value("${virtual.account.bank.code:999}")
    private String virtualAccountBankCode;

    @Value("${virtual.account.name.prefix:RENTAL}")
    private String virtualAccountNamePrefix;

    // ---- Payment provider (simulated by the mock bank) ----
    // Base URL and webhook URL are read from Environment at call time (see PaymentProviderClient /
    // MockBankService) so they can reference ${local.server.port}, which is only known after startup.

    /** API key we send when calling the provider (requery). */
    @Value("${payment.provider.secret-key}")
    private String providerSecretKey;

    /** Shared secret the provider uses to sign webhooks. */
    @Value("${payment.provider.webhook-secret}")
    private String providerWebhookSecret;

    /** How long the mock bank takes to settle a transfer. */
    @Value("${payment.provider.settlement-delay-ms:2000}")
    private long providerSettlementDelayMs;

    @Value("${payment.provider.webhook-max-attempts:5}")
    private int providerWebhookMaxAttempts;

    /** First retry delay; doubles on each further attempt. */
    @Value("${payment.provider.webhook-retry-delay-ms:2000}")
    private long providerWebhookRetryDelayMs;

    @Value("${bootstrap.staff.username}")
    private String bootstrapStaffUsername;

    @Value("${bootstrap.staff.email}")
    private String bootstrapStaffEmail;

    @Value("${bootstrap.staff.password}")
    private String bootstrapStaffPassword;
}
