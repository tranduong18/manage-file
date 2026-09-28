package com.duong.managefile.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNAUTHORIZED(1001, "Unauthorized", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1002, "Access denied", HttpStatus.FORBIDDEN),
    INTERNAL_SERVER_ERROR(1003, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST(1004, "Bad request", HttpStatus.BAD_REQUEST),

    USER_NOT_FOUND(2001, "User not found", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
