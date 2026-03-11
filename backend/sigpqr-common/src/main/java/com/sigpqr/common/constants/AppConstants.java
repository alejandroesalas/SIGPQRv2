package com.sigpqr.common.constants;

public final class AppConstants {

    private AppConstants() {}

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * Returns the best available request identifier from MDC:
     * correlationId if present, otherwise traceId from Micrometer.
     */
    public static String getRequestId() {
        String cid = org.slf4j.MDC.get(CORRELATION_ID_MDC_KEY);
        if (cid != null && !cid.isBlank()) {
            return cid;
        }
        String tid = org.slf4j.MDC.get(TRACE_ID_MDC_KEY);
        return (tid != null && !tid.isBlank()) ? tid : "no-id";
    }
}
