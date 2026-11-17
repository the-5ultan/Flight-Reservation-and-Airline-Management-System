package com.aeroreserve.service;

import com.aeroreserve.exception.BookingNotFoundException;
import com.aeroreserve.exception.PaymentProcessingException;
import com.aeroreserve.model.Payment;
import com.aeroreserve.model.PaymentMethod;
import com.aeroreserve.repository.PaymentRepository;

import java.util.List;

/** Processes simulated payments and refunds through polymorphic methods. */
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment process(String bookingId, double amount, PaymentMethod method)
            throws PaymentProcessingException {
        Payment payment = new Payment(bookingId, amount, method);
        try {
            method.process(amount, payment.getTransactionReference());
            payment.markCompleted();
        } catch (PaymentProcessingException e) {
            payment.markFailed();
            paymentRepository.put(payment.getPaymentId(), payment);
            throw e;
        }
        paymentRepository.put(payment.getPaymentId(), payment);
        return payment;
    }

    public Payment refund(String bookingId, double amount) throws PaymentProcessingException {
        Payment payment = paymentRepository.findCompletedByBooking(bookingId)
                .orElseThrow(() -> new PaymentProcessingException("No completed payment found for this booking"));
        try {
            payment.markRefunded(amount);
        } catch (IllegalArgumentException e) {
            throw new PaymentProcessingException(e.getMessage());
        }
        paymentRepository.put(payment.getPaymentId(), payment);
        return payment;
    }

    public Payment getByBooking(String bookingId) throws BookingNotFoundException {
        return paymentRepository.findByBooking(bookingId).stream()
                .findFirst()
                .orElseThrow(() -> new BookingNotFoundException("No payment record for this booking"));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
