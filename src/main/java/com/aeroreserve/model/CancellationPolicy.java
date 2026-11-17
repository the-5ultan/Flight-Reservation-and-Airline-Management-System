package com.aeroreserve.model;

import java.io.Serializable;

/** Strategy for computing cancellation charges; selected by cabin class. */
public interface CancellationPolicy extends Serializable {

    double calculateFee(Booking booking, Flight flight);

    String getPolicyName();
}
