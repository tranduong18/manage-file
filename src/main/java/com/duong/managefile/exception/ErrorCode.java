package com.duong.managefile.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    UNAUTHORIZED(1001, "Unauthorized", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1002, "Access denied", HttpStatus.FORBIDDEN),
    INTERNAL_SERVER_ERROR(1003, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST(1004, "Bad request", HttpStatus.BAD_REQUEST),
    NOT_FOUND(1005, "Resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(1006, "Method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    DATA_INTEGRITY_VIOLATION(1007, "Data conflict or constraint violation", HttpStatus.CONFLICT),

    // Google Drive
    GOOGLE_TOKEN_EXPIRED(2001, "Google access token expired", HttpStatus.UNAUTHORIZED),
    GOOGLE_CLIENT_BUILD_FAILED(2002, "Failed to build Google Drive client", HttpStatus.INTERNAL_SERVER_ERROR),
    DRIVE_API_ERROR(2003, "Google Drive API error", HttpStatus.BAD_GATEWAY),

    // User
    USER_NOT_FOUND(3001, "User not found", HttpStatus.NOT_FOUND),

    // Google Account
    GOOGLE_ACCOUNT_NOT_FOUND(4001, "Google Account not found", HttpStatus.NOT_FOUND),

    // File
    FILE_NOT_FOUND(5001, "File not found", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
