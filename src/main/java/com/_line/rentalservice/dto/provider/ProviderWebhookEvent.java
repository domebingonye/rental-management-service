package com._line.rentalservice.dto.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderWebhookEvent {
    public static final String TRANSFER_SUCCESSFUL = "transfer.successful";
    public static final String TRANSFER_FAILED = "transfer.failed";

    private String event;
    private ProviderTransaction data;
}
