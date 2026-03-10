package com.sigpqr.common.constants;

public final class RabbitMQConstants {

    private RabbitMQConstants() {}

    // Exchanges
    public static final String USER_EVENTS_EXCHANGE = "user.events";
    public static final String AUTH_EVENTS_EXCHANGE = "auth.events";
    public static final String PQR_EVENTS_EXCHANGE = "pqr.events";

    // Routing Keys
    public static final String USER_REGISTERED_KEY = "user.registered";
    public static final String AUTH_PASSWORD_RESET_KEY = "auth.password-reset";
    public static final String PQR_STATUS_CHANGED_KEY = "pqr.status-changed";
    public static final String PQR_RESPONSE_CREATED_KEY = "pqr.response-created";
    public static final String PQR_ATTACHMENT_DELETED_KEY = "pqr.attachment-deleted";
}
