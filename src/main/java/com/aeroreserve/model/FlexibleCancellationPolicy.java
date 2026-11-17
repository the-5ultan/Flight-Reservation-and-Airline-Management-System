package com.aeroreserve.model;

/** Flexible terms offered to first-class passengers. */
public class FlexibleCancellationPolicy implements CancellationPolicy {

    private static final long serialVersionUID = 1L;

    @Override
    public double calculateFee(Booking booking, Flight flight) {
        long hours = StandardCancellationPolicy.hoursBeforeDeparture(flight);
        double rate = hours > 24 ? 0.0 : 0.10;
        return Math.round(booking.getTotalAmount() * rate * 100.0) / 100.0;
    }

    @Override
    public String getPolicyName() {
        return "Flexible cancellation policy";
    }
}
