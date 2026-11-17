package com.aeroreserve.service;

import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.generic.CustomList;
import com.aeroreserve.model.Booking;
import com.aeroreserve.model.DashboardStatistics;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.Payment;
import com.aeroreserve.repository.BookingRepository;
import com.aeroreserve.repository.FlightRepository;
import com.aeroreserve.repository.PaymentRepository;
import com.aeroreserve.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Computes every dashboard figure from live repository data. */
public class ReportService {

    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public ReportService(FlightRepository flightRepository, BookingRepository bookingRepository,
                         PaymentRepository paymentRepository, UserRepository userRepository) {
        this.flightRepository = flightRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    public DashboardStatistics computeDashboard() {
        List<Flight> flights = flightRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        int available = 0;
        int occupied = 0;
        for (Flight flight : flights) {
            available += flight.getAvailableSeatCount();
            occupied += flight.getOccupiedSeatCount();
        }
        int capacity = available + occupied;

        double revenue = 0;
        double refunds = 0;
        for (Payment payment : payments) {
            if (payment.getStatus().name().startsWith("COMPLETED")
                    || payment.getStatus().name().startsWith("PARTIALLY")
                    || payment.getStatus().name().startsWith("REFUNDED")) {
                revenue += payment.getAmount() - payment.getRefundedAmount();
                refunds += payment.getRefundedAmount();
            }
        }

        int active = 0;
        int cancelled = 0;
        Map<String, Integer> routes = new LinkedHashMap<>();
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                cancelled++;
            } else {
                active++;
                flightRepository.findById(booking.getFlightId()).ifPresent(flight ->
                        routes.merge(flight.getRoute(), 1, Integer::sum));
            }
        }
        Map<String, Integer> popularRoutes = new LinkedHashMap<>();
        routes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(6)
                .forEach(entry -> popularRoutes.put(entry.getKey(), entry.getValue()));

        return new DashboardStatistics(
                flights.size(),
                (int) userRepository.findAll().stream()
                        .filter(user -> user.getRole() == com.aeroreserve.enums.UserRole.PASSENGER).count(),
                bookings.size(),
                active,
                cancelled,
                available,
                occupied,
                Math.round(revenue * 100.0) / 100.0,
                Math.round(refunds * 100.0) / 100.0,
                capacity == 0 ? 0 : Math.round((occupied * 1000.0 / capacity)) / 10.0,
                new CustomList<>(bookingRepository.findRecent(6)),
                new CustomList<>(flightRepository.findUpcoming().stream().limit(6).toList()),
                popularRoutes);
    }
}
