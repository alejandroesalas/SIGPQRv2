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

    public static Profile fromId(int id) {
        for (Profile p : values()) {
            if (p.id == id) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown profile id: " + id);
    }
}
