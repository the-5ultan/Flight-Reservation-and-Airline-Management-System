package com.aeroreserve.model;

import com.aeroreserve.enums.PaymentType;
import com.aeroreserve.exception.PaymentProcessingException;

import java.io.Serializable;

/** Abstract payment strategy. Subclasses implement polymorphic processing. */
public abstract class PaymentMethod implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String displayName;

    protected PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public abstract PaymentType getPaymentType();

    public abstract String getDetails();

    public abstract void process(double amount, String transactionReference) throws PaymentProcessingException;

    public String getDisplayName() {
        return displayName;
    }
}
