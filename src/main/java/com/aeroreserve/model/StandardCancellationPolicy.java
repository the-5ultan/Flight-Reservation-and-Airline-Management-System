package com.aeroreserve.model;

import java.time.Duration;
import java.time.LocalDateTime;

/** Standard economy/business cancellation terms. */
public class StandardCancellationPolicy implements CancellationPolicy {

    private static final long serialVersionUID = 1L;

    @Override
    public double calculateFee(Booking booking, Flight flight) {
        long hours = hoursBeforeDeparture(flight);
        double rate = hours > 168 ? 0.10 : hours > 48 ? 0.25 : hours > 24 ? 0.40 : 0.60;
        return Math.round(booking.getTotalAmount() * rate * 100.0) / 100.0;
    }

    @Override
    public String getPolicyName() {
        return "Standard cancellation policy";
    }

    static long hoursBeforeDeparture(Flight flight) {
        return Duration.between(LocalDateTime.now(), flight.getDepartureDateTime()).toHours();
    }
}
