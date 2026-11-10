package com.threelinelimited.rentalservice.exception;

import com.threelinelimited.rentalservice.enums.ResponseCodes;

public class RequestException extends RuntimeException {
    private final String code;

    public RequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public RequestException(ResponseCodes responseCode, String message) {
        this(responseCode.getCode(), message);
    }

    public String getCode() {
        return this.code;
    }
}
