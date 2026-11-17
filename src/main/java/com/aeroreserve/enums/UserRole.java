package com.aeroreserve.enums;

/** Roles supported by the application. */
public enum UserRole {
    ADMIN("Administrator"),
    PASSENGER("Passenger");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
