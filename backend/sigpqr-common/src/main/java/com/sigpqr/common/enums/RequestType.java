package com.sigpqr.common.enums;

public enum RequestType {

    PETITION(1),
    COMPLAINT(2),
    CLAIM(3);

    private final int id;

    RequestType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
