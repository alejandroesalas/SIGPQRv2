package com.sigpqr.common.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String message,
        Map<String, String> details,
        String correlationId,
        LocalDateTime timestamp
) {
}
