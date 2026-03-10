package com.sigpqr.common.dto;

import com.sigpqr.common.constants.AppConstants;
import org.slf4j.MDC;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String correlationId,
        LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, MDC.get(AppConstants.CORRELATION_ID_MDC_KEY), LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, MDC.get(AppConstants.CORRELATION_ID_MDC_KEY), LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, MDC.get(AppConstants.CORRELATION_ID_MDC_KEY), LocalDateTime.now());
    }
}
