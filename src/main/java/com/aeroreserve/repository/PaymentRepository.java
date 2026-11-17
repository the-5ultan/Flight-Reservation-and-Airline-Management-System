package com.aeroreserve.repository;

import com.aeroreserve.model.Payment;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Payment persistence. */
public class PaymentRepository extends SerializedRepository<Payment> {

    public PaymentRepository(Path file) {
        super(file);
    }

    public List<Payment> findByBooking(String bookingId) {
        return findAll().stream()
                .filter(payment -> payment.getBookingId().equals(bookingId))
                .toList();
    }

    public Optional<Payment> findCompletedByBooking(String bookingId) {
        return findByBooking(bookingId).stream()
                .filter(Payment::isCompleted)
                .findFirst();
    }
}
