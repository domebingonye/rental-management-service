package com.threelinelimited.rentalservice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS("00", "Success"),
    FAILURE("01", "Failure"),
    BAD_REQUEST("400", "Bad request"),
    UNAUTHORIZED("401", "Unauthorized"),
    FORBIDDEN("403", "Forbidden"),
    NOT_FOUND("404", "Not found"),
    CONFLICT("409", "Conflict"),
    INTERNAL_SERVER_ERROR("500", "Internal Server Error"),
    BAD_GATEWAY("502", "Upstream provider unavailable");

    @Getter
    private final String code;
    @Getter
    private final String message;
}
