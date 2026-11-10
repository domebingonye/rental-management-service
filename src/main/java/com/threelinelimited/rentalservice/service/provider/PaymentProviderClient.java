package com.threelinelimited.rentalservice.service.provider;

import com.threelinelimited.rentalservice.config.PropsReader;
import com.threelinelimited.rentalservice.constant.ApplicationConstants;
import com.threelinelimited.rentalservice.dto.provider.ProviderTransaction;
import com.threelinelimited.rentalservice.enums.ResponseCodes;
import com.threelinelimited.rentalservice.exception.RequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentProviderClient {
    private final PropsReader propsReader;
    private final Environment environment;
    private final RestClient restClient = RestClient.builder().requestFactory(timeouts()).build();

    public Optional<ProviderTransaction> requery(String sessionId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(baseUrl() + "/api/v1/transactions/{sessionId}", sessionId)
                    .header(ApplicationConstants.AUTHORIZATION_KEY, ApplicationConstants.BEARER_PREFIX + propsReader.getProviderSecretKey())
                    .retrieve()
                    .body(ProviderTransaction.class));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return Optional.empty();
            log.error("Provider requery for {} rejected: {}", sessionId, e.getStatusCode());
            throw new RequestException(ResponseCodes.BAD_GATEWAY, "Payment provider rejected the requery");
        } catch (RestClientException e) {
            log.error("Provider requery for {} failed: {}", sessionId, e.getMessage());
            throw new RequestException(ResponseCodes.BAD_GATEWAY, "Payment provider unavailable");
        }
    }

    private String baseUrl() {
        String url = environment.getProperty("payment.provider.base-url");
        if (!StringUtils.hasText(url)) throw new IllegalStateException("payment.provider.base-url is not set");
        return url;
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return factory;
    }
}
