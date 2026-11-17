package com.aeroreserve.service;

import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.enums.FlightStatus;
import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.exception.AircraftUnavailableException;
import com.aeroreserve.exception.FlightNotFoundException;
import com.aeroreserve.exception.InvalidFlightScheduleException;
import com.aeroreserve.model.Booking;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.FlightSearchCriteria;
import com.aeroreserve.model.Payment;
import com.aeroreserve.model.User;
import com.aeroreserve.repository.AircraftRepository;
import com.aeroreserve.repository.BookingRepository;
import com.aeroreserve.repository.FlightRepository;
import com.aeroreserve.repository.PaymentRepository;
import com.aeroreserve.repository.UserRepository;
import com.aeroreserve.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Flight scheduling, filtering and safe cancellation. */
public class FlightService {

    private final FlightRepository flightRepository;
    private final AircraftRepository aircraftRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public FlightService(FlightRepository flightRepository, AircraftRepository aircraftRepository,
                         BookingRepository bookingRepository, PaymentRepository paymentRepository,
                         UserRepository userRepository) {
        this.flightRepository = flightRepository;
        this.aircraftRepository = aircraftRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    public Flight createFlight(String flightNumber, String aircraftId, String from, String to,
                               LocalDateTime departure, LocalDateTime arrival, double basePrice,
                               FlightStatus status) throws InvalidFlightScheduleException, AircraftUnavailableException {
        String number = flightNumber == null ? "" : flightNumber.trim().toUpperCase();
        if (number.isBlank()) {
            throw new IllegalArgumentException("Flight number is required");
        }
        if (flightRepository.findByFlightNumber(number).isPresent()) {
            throw new IllegalArgumentException("Flight number " + number + " already exists");
        }
        Flight.validateSchedule(from, to, departure, arrival, basePrice);
        var aircraft = aircraftRepository.findById(aircraftId)
                .orElseThrow(() -> new AircraftUnavailableException("Selected aircraft does not exist"));
        Flight flight = new Flight(IdGenerator.generate("FL"), number, aircraftId, from, to,
                departure, arrival, basePrice, status);
        flight.assignAircraft(aircraft);
        flightRepository.put(flight.getFlightId(), flight);
        return flight;
    }

    public void updateFlight(String flightId, String flightNumber, String from, String to,
                             LocalDateTime departure, LocalDateTime arrival, double basePrice,
                             FlightStatus status) throws FlightNotFoundException, InvalidFlightScheduleException {
        Flight flight = getFlight(flightId);
        var duplicate = flightRepository.findByFlightNumber(flightNumber);
        if (duplicate.isPresent() && !duplicate.get().getFlightId().equals(flightId)) {
            throw new IllegalArgumentException("Flight number " + flightNumber + " already exists");
        }
        flight.updateSchedule(flightNumber, from, to, departure, arrival, basePrice, status);
        flightRepository.put(flightId, flight);
    }

    public void deleteFlight(String flightId) throws FlightNotFoundException {
        Flight flight = getFlight(flightId);
        long activeBookings = bookingRepository.findActiveByFlight(flightId).stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                .count();
        if (activeBookings > 0) {
            throw new IllegalStateException("Cannot delete a flight with " + activeBookings
                    + " active booking(s). Cancel the flight instead.");
        }
        flightRepository.remove(flight.getFlightId());
    }

    /** Cancels a flight, refunds every active booking and frees its seats. */
    public synchronized int cancelFlight(String flightId) throws FlightNotFoundException {
        Flight flight = getFlight(flightId);
        if (flight.getStatus() == FlightStatus.CANCELLED) {
            return 0;
        }
        flight.setStatus(FlightStatus.CANCELLED);
        flightRepository.put(flightId, flight);

        int affected = 0;
        for (Booking booking : bookingRepository.findActiveByFlight(flightId)) {
            flight.releaseSeats(booking.getSeatNumbers(), booking.getBookingId());
            double refund = booking.getTotalAmount();
            booking.cancel(0.0, refund);
            bookingRepository.put(booking.getBookingId(), booking);
            paymentRepository.findCompletedByBooking(booking.getBookingId())
                    .ifPresent(payment -> {
                        payment.markRefunded(refund);
                        paymentRepository.put(payment.getPaymentId(), payment);
                    });
            detachFromPassenger(booking);
            affected++;
        }
        flightRepository.put(flightId, flight);
        return affected;
    }

    private void detachFromPassenger(Booking booking) {
        userRepository.findById(booking.getPassengerId()).ifPresent(user -> {
            if (user instanceof com.aeroreserve.model.Passenger passenger) {
                passenger.removeBookingId(booking.getBookingId());
                userRepository.put(user.getUserId(), user);
            }
        });
    }

    public Flight getFlight(String flightId) throws FlightNotFoundException {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found"));
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAllByDeparture();
    }

    public List<Flight> searchFlights(FlightSearchCriteria criteria) {
        return flightRepository.findAll().stream()
                .filter(flight -> criteria.matchesRoute(flight))
                .filter(Flight::isBookable)
                .filter(flight -> criteria.getStatus() == null || flight.getStatus() == criteria.getStatus())
                .filter(flight -> criteria.getTravelDate() == null
                        || flight.getDepartureDateTime().toLocalDate().equals(criteria.getTravelDate()))
                .filter(flight -> flight.getAvailableSeatCount() >= criteria.getPassengerCount())
                .filter(flight -> criteria.getMinAvailableSeats() == null
                        || flight.getAvailableSeatCount() >= criteria.getMinAvailableSeats())
                .filter(flight -> criteria.getMaxPrice() == null || flight.getBasePrice() <= criteria.getMaxPrice())
                .sorted(comparatorFor(criteria.getSortBy()))
                .toList();
    }

    private Comparator<Flight> comparatorFor(FlightSearchCriteria.SortOption option) {
        return switch (option) {
            case DEPARTURE_DESC -> Comparator.comparing(Flight::getDepartureDateTime).reversed();
            case PRICE_ASC -> Comparator.comparingDouble(Flight::getBasePrice);
            case PRICE_DESC -> Comparator.comparingDouble(Flight::getBasePrice).reversed();
            case SEATS_ASC -> Comparator.comparingInt(Flight::getAvailableSeatCount);
            case DEPARTURE_ASC -> Comparator.comparing(Flight::getDepartureDateTime);
        };
    }

    public List<Flight> getUpcomingFlights(int limit) {
        return flightRepository.findUpcoming().stream().limit(limit).toList();
    }

    public double getLowestFare(Flight flight) {
        double lowest = flight.getFare(SeatClass.FIRST);
        for (SeatClass seatClass : SeatClass.values()) {
            lowest = Math.min(lowest, flight.getFare(seatClass));
        }
        return lowest;
    }

    public List<Payment> getPaymentsForFlight(String flightId) {
        return bookingRepository.findByFlight(flightId).stream()
                .flatMap(booking -> paymentRepository.findByBooking(booking.getBookingId()).stream())
                .toList();
    }

    public User findPassengerById(String passengerId) {
        return userRepository.findById(passengerId).orElse(null);
    }
}
