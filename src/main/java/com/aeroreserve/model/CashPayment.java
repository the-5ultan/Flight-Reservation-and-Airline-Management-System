package com.aeroreserve.model;

import com.aeroreserve.enums.PaymentType;
import com.aeroreserve.exception.PaymentProcessingException;
import com.aeroreserve.util.ValidationUtil;

/** Simulated cash payment collected at an airline counter. */
public class CashPayment extends PaymentMethod {

    private static final long serialVersionUID = 1L;

    private final String counterLocation;

    public CashPayment(String counterLocation) {
        super("Cash payment");
        this.counterLocation = ValidationUtil.requireText(counterLocation, "Counter location");
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.CASH;
    }

    @Override
    public String getDetails() {
        return "Counter: " + counterLocation;
    }

    @Override
    public void process(double amount, String transactionReference) throws PaymentProcessingException {
        if (!ValidationUtil.isPositive(amount)) {
            throw new PaymentProcessingException("Payment amount must be positive");
        }
    }

    public String getCounterLocation() {
        return counterLocation;
    }
}
