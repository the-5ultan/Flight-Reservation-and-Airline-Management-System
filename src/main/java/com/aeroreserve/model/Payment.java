package com.aeroreserve.model;

import com.aeroreserve.enums.PaymentStatus;
import com.aeroreserve.enums.PaymentType;
import com.aeroreserve.util.IdGenerator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Persistent record of a simulated transaction and any refund. */
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String paymentId;
    private final String transactionReference;
    private final String bookingId;
    private final double amount;
    private final PaymentType paymentType;
    private final String methodDetails;
    private final LocalDateTime paymentDate;

    private PaymentStatus status = PaymentStatus.PENDING;
    private double refundedAmount;
    private LocalDateTime refundDate;

    public Payment(String bookingId, double amount, PaymentMethod method) {
        this(IdGenerator.generate("PAY"), bookingId, amount, method);
    }

    public Payment(String paymentId, String bookingId, double amount, PaymentMethod method) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId");
        this.bookingId = Objects.requireNonNull(bookingId, "bookingId");
        this.amount = amount;
        this.paymentType = method.getPaymentType();
        this.methodDetails = method.getDetails();
        this.transactionReference = "TXN-" + paymentId.substring(paymentId.indexOf('-') + 1);
        this.paymentDate = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = PaymentStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markRefunded(double amount) {
        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only completed payments can be refunded");
        }
        if (amount < 0 || amount > this.amount) {
            throw new IllegalArgumentException("Invalid refund amount");
        }
        this.refundedAmount = Math.round(amount * 100.0) / 100.0;
        this.refundDate = LocalDateTime.now();
        this.status = this.refundedAmount >= this.amount
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getBookingId() {
        return bookingId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public String getMethodDetails() {
        return methodDetails;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public double getRefundedAmount() {
        return refundedAmount;
    }

    public LocalDateTime getRefundDate() {
        return refundDate;
    }

    @Override
    public String toString() {
        return transactionReference + " · " + paymentType.getDisplayName() + " · " + status.getDisplayName();
    }
}
