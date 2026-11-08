package com._line.rentalservice.dto.provider;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NameEnquiryResponse {
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private String bankName;
}
