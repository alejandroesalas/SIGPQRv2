package com.sigpqr.common.enums;

public enum Profile {

    ADMIN(1),
    COORDINATOR(2),
    STUDENT(3),
    TEACHER(4);

    private final int id;

    Profile(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
