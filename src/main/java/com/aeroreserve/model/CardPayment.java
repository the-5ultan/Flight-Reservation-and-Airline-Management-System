package com.aeroreserve.model;

import com.aeroreserve.enums.PaymentType;
import com.aeroreserve.exception.PaymentProcessingException;
import com.aeroreserve.util.ValidationUtil;

import java.time.YearMonth;

/** Simulated card payment. Only a masked number is retained. */
public class CardPayment extends PaymentMethod {

    private static final long serialVersionUID = 1L;

    private final String cardHolder;
    private final String maskedNumber;
    private final YearMonth expiry;

    public CardPayment(String cardHolder, String cardNumber, YearMonth expiry) {
        super("Card payment");
        this.cardHolder = ValidationUtil.requireText(cardHolder, "Card holder");
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        if (digits.length() < 12 || digits.length() > 19) {
            throw new IllegalArgumentException("Enter a valid card number");
        }
        this.maskedNumber = "**** **** **** " + digits.substring(digits.length() - 4);
        this.expiry = java.util.Objects.requireNonNull(expiry, "expiry");
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.CARD;
    }

    @Override
    public String getDetails() {
        return maskedNumber + " · " + cardHolder + " · exp " + expiry;
    }

    @Override
    public void process(double amount, String transactionReference) throws PaymentProcessingException {
        if (!ValidationUtil.isPositive(amount)) {
            throw new PaymentProcessingException("Payment amount must be positive");
        }
        if (expiry.isBefore(YearMonth.now())) {
            throw new PaymentProcessingException("The card has expired");
        }
    }

    public String getMaskedNumber() {
        return maskedNumber;
    }
}
