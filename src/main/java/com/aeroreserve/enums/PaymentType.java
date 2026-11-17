package com.aeroreserve.enums;

/** Supported simulated payment methods. */
public enum PaymentType {
    CARD("Card payment"),
    CASH("Cash payment");

    private final String displayName;

    PaymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
