package com.aeroreserve;

import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.enums.FlightStatus;
import com.aeroreserve.enums.PaymentStatus;
import com.aeroreserve.enums.PaymentType;
import com.aeroreserve.exception.AuthenticationException;
import com.aeroreserve.exception.DuplicateUserException;
import com.aeroreserve.exception.InsufficientSeatsException;
import com.aeroreserve.exception.InvalidFlightScheduleException;
import com.aeroreserve.exception.PaymentProcessingException;
import com.aeroreserve.exception.SeatAlreadyBookedException;
import com.aeroreserve.exception.UnauthorizedActionException;
import com.aeroreserve.generic.CustomList;
import com.aeroreserve.model.Aircraft;
import com.aeroreserve.model.Booking;
import com.aeroreserve.model.BookingResult;
import com.aeroreserve.model.CardPayment;
import com.aeroreserve.model.CashPayment;
import com.aeroreserve.model.DashboardStatistics;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.FlightSearchCriteria;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.model.Payment;
import com.aeroreserve.model.PaymentMethod;
import com.aeroreserve.model.User;
import com.aeroreserve.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceFlowTest {

    @TempDir
    Path tempDir;

    @Test
    void authenticationRegistersLogsInAndEnforcesRoles() throws Exception {
        TestFixtures.Env env = TestFixtures.env(tempDir);
        Passenger passenger = TestFixtures.passenger(env, "user@example.com");
        assertEquals("user@example.com", passenger.getEmail());
        assertTrue(PasswordUtil.verify("Passw0rd!", passenger.getPasswordHash()));

        assertThrows(DuplicateUserException.class, () -> TestFixtures.passenger(env, "user@example.com"));
        assertThrows(AuthenticationException.class, () -> env.auth.registerPassenger(
                "Weak User", "weak@example.com", "weakpass", "+1 555 0101", "P-2", "Testland"));

        User loggedIn = env.auth.login(" USER@EXAMPLE.COM ", "Passw0rd!");
        assertEquals(passenger.getUserId(), loggedIn.getUserId());
        assertTrue(env.auth.isLoggedIn());
        assertThrows(UnauthorizedActionException.class, () -> env.auth.requireAdmin());

        env.auth.ensureDefaultAdmin("admin@example.com", "AdminPass123", "Admin User");
        env.auth.login("admin@example.com", "AdminPass123");
        assertDoesNotThrow(() -> env.auth.requireAdmin());
    }

    @Test
    void flightValidationAndSearchFilterCorrectly() throws Exception {
        TestFixtures.Env env = TestFixtures.env(tempDir);
        Aircraft aircraft = TestFixtures.aircraft(env);
        LocalDateTime departure = LocalDateTime.now().plusDays(6);
        LocalDateTime arrival = departure.plusHours(2);

        assertThrows(InvalidFlightScheduleException.class, () -> env.flights.createFlight(
                "AR200", aircraft.getAircraftId(), "JFK", "JFK", departure, arrival, 400.0,
                FlightStatus.SCHEDULED));

        Flight first = env.flights.createFlight("AR201", aircraft.getAircraftId(), "jfk", "lhr",
                departure, arrival, 400.0, FlightStatus.SCHEDULED);
        Flight second = env.flights.createFlight("AR202", aircraft.getAircraftId(), "JFK", "LHR",
                departure.plusHours(3), arrival.plusHours(3), 420.0, FlightStatus.SCHEDULED);
        env.flights.createFlight("AR203", aircraft.getAircraftId(), "LHR", "CDG",
                departure, arrival, 300.0, FlightStatus.SCHEDULED);

        FlightSearchCriteria criteria = new FlightSearchCriteria("jfk", "lhr",
                departure.toLocalDate(), 1);
        List<Flight> results = env.flights.searchFlights(criteria);
        assertEquals(2, results.size());
        assertTrue(results.contains(first));
        assertTrue(results.contains(second));

        criteria.setStatus(FlightStatus.CANCELLED);
        assertTrue(env.flights.searchFlights(criteria).isEmpty());
    }

    @Test
    void seatReservationAndReleaseUpdateFlightInventory() throws Exception {
        TestFixtures.Env env = TestFixtures.env(tempDir);
        Flight flight = TestFixtures.flight(env, "AR201", "JFK", "LHR", 6);
        CustomList<String> seats = new CustomList<>(List.of("3A", "3B"));
        int before = flight.getAvailableSeatCount();

        env.seats.reserveSeats(flight.getFlightId(), seats, "BK-1");
        assertEquals(before - 2, flight.getAvailableSeatCount());
        assertTrue(flight.getBookingIds().contains("BK-1"));
        assertThrows(SeatAlreadyBookedException.class, () -> env.seats.reserveSeats(
                flight.getFlightId(), new CustomList<>(List.of("3A")), "BK-2"));
        assertThrows(InsufficientSeatsException.class, () -> env.seats.reserveSeats(
                flight.getFlightId(), new CustomList<>(), "BK-3"));

        env.seats.releaseSeats(flight.getFlightId(), seats, "BK-1");
        assertEquals(before, flight.getAvailableSeatCount());
        assertFalse(flight.getBookingIds().contains("BK-1"));
    }

    @Test
    void bookingHappyPathConfirmsPaymentSeatsAndTicket() throws Exception {
        TestFixtures.Env env = TestFixtures.env(tempDir);
        Passenger passenger = TestFixtures.passenger(env, "buyer@example.com");
        Flight flight = TestFixtures.flight(env, "AR201", "JFK", "LHR", 8);

        BookingResult result = env.bookings.createBooking(passenger.getUserId(), flight.getFlightId(),
                new CustomList<>(List.of("3A", "3B")), new CashPayment("Counter A"));

        assertEquals(BookingStatus.CONFIRMED, result.getBooking().getStatus());
        assertEquals(PaymentStatus.COMPLETED, result.getPayment().getStatus());
        assertEquals(PaymentType.CASH, result.getPayment().getPaymentType());
        assertTrue(result.getTicket().getSeatList().contains("3A"));
        assertEquals(result.getBooking().getTotalAmount(), result.getTicket().getTotalFare(), 0.001);
        assertEquals(2, flight.getOccupiedSeatCount());
        assertTrue(passenger.getBookingIds().contains(result.getBooking().getBookingId()));

        assertThrows(SeatAlreadyBookedException.class, () -> env.bookings.createBooking(
                passenger.getUserId(), flight.getFlightId(), new CustomList<>(List.of("3A")),
                new CashPayment("Counter A")));
    }

    @Test
    void bookingRollsBackSeatsAndReservationWhenPaymentFails() throws Exception {
        TestFixtures.Env env = TestFixtures.env(tempDir);
        Passenger passenger = TestFixtures.passenger(env, "buyer@example.com");
        Flight flight = TestFixtures.flight(env, "AR201", "JFK", "LHR", 8);

        assertThrows(PaymentProcessingException.class, () -> env.bookings.createBooking(
                passenger.getUserId(), flight.getFlightId(), new CustomList<>(List.of("3A")),
                new FailingPayment()));

        assertTrue(env.bookings.getAllBookings().isEmpty());
        assertEquals(0, flight.getOccupiedSeatCount());
        assertTrue(flight.getBookingIds().isEmpty());
        assertEquals(1, env.payments.getAllPayments().size());
    }

    @Test
    void cancellationAppliesPolicyFreesSeatsAndRefunds() throws Exception {
        TestFixtures.Env env = TestFixtures.env(tempDir);
        Passenger passenger = TestFixtures.passenger(env, "buyer@example.com");
        Flight flight = TestFixtures.flight(env, "AR201", "JFK", "LHR", 10);
        BookingResult result = env.bookings.createBooking(passenger.getUserId(), flight.getFlightId(),
                new CustomList<>(List.of("3A")), new CashPayment("Counter A"));
        String bookingId = result.getBooking().getBookingId();
        double expectedFee = Math.round(result.getBooking().getTotalAmount() * 0.10 * 100.0) / 100.0;

        Booking cancelled = env.bookings.cancelBooking(bookingId, passenger.getUserId(), false);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(expectedFee, cancelled.getCancellationFee(), 0.001);
        assertEquals(result.getBooking().getTotalAmount() - expectedFee,
                cancelled.getRefundAmount(), 0.001);
        assertEquals(0, flight.getOccupiedSeatCount());
        assertFalse(passenger.getBookingIds().contains(bookingId));
        assertThrows(IllegalStateException.class,
                () -> env.bookings.cancelBooking(bookingId, passenger.getUserId(), false));
    }

    @Test
    void paymentProcessingAndRefundTrackStatuses() throws Exception {
        TestFixtures.Env env = TestFixtures.env(tempDir);

        Payment payment = env.payments.process("BK-1", 250.0, new CashPayment("Counter A"));
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("Counter: Counter A", payment.getMethodDetails());

        Payment partial = env.payments.refund("BK-1", 100.0);
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, partial.getStatus());
        assertEquals(100.0, partial.getRefundedAmount(), 0.001);
        assertThrows(PaymentProcessingException.class, () -> env.payments.refund("BK-1", 200.0));
        assertThrows(PaymentProcessingException.class, () -> env.payments.process("BK-2", 10.0,
                new CardPayment("A B", "4111111111111111", YearMonth.now().minusMonths(1))));
    }

    @Test
    void reportAggregatesLiveData() throws Exception {
        TestFixtures.Env env = TestFixtures.env(tempDir);
        Passenger passenger = TestFixtures.passenger(env, "viewer@example.com");
        Flight flight = TestFixtures.flight(env, "AR201", "JFK", "LHR", 8);
        BookingResult result = env.bookings.createBooking(passenger.getUserId(), flight.getFlightId(),
                new CustomList<>(List.of("3A")), new CashPayment("Counter A"));

        DashboardStatistics stats = env.reports.computeDashboard();

        assertEquals(1, stats.getTotalFlights());
        assertEquals(1, stats.getTotalPassengers());
        assertEquals(1, stats.getActiveBookings());
        assertEquals(1, stats.getOccupiedSeats());
        assertEquals(21, stats.getAvailableSeats());
        assertEquals(result.getBooking().getTotalAmount(), stats.getRevenue(), 0.001);
        assertEquals("JFK → LHR", stats.getPopularRoutes().keySet().iterator().next());
    }

    @Test
    void flightCancellationRefundsBookingsWithoutFee() throws Exception {
        TestFixtures.Env env = TestFixtures.env(tempDir);
        Passenger passenger = TestFixtures.passenger(env, "buyer@example.com");
        Flight flight = TestFixtures.flight(env, "AR201", "JFK", "LHR", 8);
        BookingResult result = env.bookings.createBooking(passenger.getUserId(), flight.getFlightId(),
                new CustomList<>(List.of("3A")), new CashPayment("Counter A"));
        String bookingId = result.getBooking().getBookingId();

        assertEquals(1, env.flights.cancelFlight(flight.getFlightId()));

        Booking cancelled = env.bookings.getBooking(bookingId);
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0.0, cancelled.getCancellationFee(), 0.001);
        assertEquals(cancelled.getTotalAmount(), cancelled.getRefundAmount(), 0.001);
        assertEquals(PaymentStatus.REFUNDED, env.payments.getByBooking(bookingId).getStatus());
        assertEquals(0, flight.getOccupiedSeatCount());
        assertEquals(0, env.flights.cancelFlight(flight.getFlightId()));
    }

    private static final class FailingPayment extends PaymentMethod {

        private FailingPayment() {
            super("Failing payment");
        }

        @Override
        public PaymentType getPaymentType() {
            return PaymentType.CASH;
        }

        @Override
        public String getDetails() {
            return "Simulated failure";
        }

        @Override
        public void process(double amount, String transactionReference) throws PaymentProcessingException {
            throw new PaymentProcessingException("Payment declined");
        }
    }
}
