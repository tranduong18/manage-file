package com.duong.managefile.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
@Slf4j(topic = "GLOBAL-EXCEPTION-HANDLER")
public class GlobalExceptionHandler {
    // Lỗi nghiệp vụ tự ném
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException exception, WebRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        return buildErrorResponse(errorCode, errorCode.getMessage(), request);
    }

    // Validate @Valid trên body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, WebRequest request){
        BindingResult bindingResult = exception.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        List<String> errors = fieldErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
        String message = errors.size() > 1 ? String.join(", ", errors) : errors.getFirst();

        return buildErrorResponse(ErrorCode.BAD_REQUEST, message, request);
    }

    // Body sai JSON, thiếu param, sai kiểu dữ liệu
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, WebRequest request) {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        return buildErrorResponse(errorCode, errorCode.getMessage(), request);
    }

    // URL không tồn tại
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception exception, WebRequest request) {
        ErrorCode errorCode = ErrorCode.NOT_FOUND;
        return buildErrorResponse(errorCode, errorCode.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, WebRequest request) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        return buildErrorResponse(errorCode, errorCode.getMessage(), request);
    }

    // Trùng unique, vi phạm FK...
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception, WebRequest request) {
        log.warn("Data integrity violation: {}", exception.getMostSpecificCause().getMessage());
        ErrorCode errorCode = ErrorCode.DATA_INTEGRITY_VIOLATION;
        return buildErrorResponse(errorCode, errorCode.getMessage(), request);
    }

    // Ném từ @PreAuthorize trong controller/service (tầng filter đã có JwtAccessDeniedHandler)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        return buildErrorResponse(errorCode, errorCode.getMessage(), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
            MissingRequestHeaderException exception, WebRequest request) {
        String message = "Required header '" + exception.getHeaderName() + "' is missing";
        return buildErrorResponse(ErrorCode.BAD_REQUEST, message, request);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, WebRequest request){
        log.error("Exception: {}", exception.getMessage(), exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return buildErrorResponse(errorCode, errorCode.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(ErrorCode errorCode, String message, WebRequest request){
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(System.currentTimeMillis())
                .code(errorCode.getCode())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }
}
