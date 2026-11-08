package com._line.rentalservice.exception;

import com._line.rentalservice.dto.response.BaseResponse;
import com._line.rentalservice.enums.ResponseCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class ExceptionAdvice {

    @ExceptionHandler(RequestException.class)
    public ResponseEntity<BaseResponse> handleException(RequestException e) {
        return ResponseEntity.status(Integer.parseInt(e.getCode())).body(build(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleException(MethodArgumentNotValidException e) {
        String message = ResponseCodes.BAD_REQUEST.getMessage();
        var errors = e.getBindingResult().getFieldErrors();
        if (!errors.isEmpty()) {
            message += ". " + errors.getFirst().getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(build(ResponseCodes.BAD_REQUEST.getCode(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse> handleException(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(build(ResponseCodes.BAD_REQUEST.getCode(), "Malformed request body"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<BaseResponse> handleException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(build(String.valueOf(e.getStatusCode().value()), e.getReason()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<BaseResponse> handleException(AuthorizationDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(build(ResponseCodes.FORBIDDEN.getCode(), e.getMessage()));
    }

    /** Two sign-ups racing for the same username/email both pass the exists-check; the unique index catches it. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse> handleException(DataIntegrityViolationException e) {
        log.warn("Constraint violation: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(build(ResponseCodes.CONFLICT.getCode(), "Username or email already exists"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleException(Exception e) {
        log.error("Error:::", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(build(ResponseCodes.INTERNAL_SERVER_ERROR.getCode(),
                StringUtils.hasText(e.getMessage()) ? e.getMessage() : ResponseCodes.INTERNAL_SERVER_ERROR.getMessage()));
    }

    private static BaseResponse build(String code, String message) {
        return BaseResponse.builder().code(code).message(message).build();
    }
}
