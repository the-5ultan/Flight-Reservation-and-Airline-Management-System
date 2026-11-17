package com.aeroreserve.service;

import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.exception.BookingNotFoundException;
import com.aeroreserve.exception.FlightNotFoundException;
import com.aeroreserve.exception.InsufficientSeatsException;
import com.aeroreserve.exception.PaymentProcessingException;
import com.aeroreserve.exception.SeatAlreadyBookedException;
import com.aeroreserve.exception.UnauthorizedActionException;
import com.aeroreserve.generic.CustomList;
import com.aeroreserve.model.Booking;
import com.aeroreserve.model.BookingResult;
import com.aeroreserve.model.FlexibleCancellationPolicy;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.model.PaymentMethod;
import com.aeroreserve.model.StandardCancellationPolicy;
import com.aeroreserve.model.Ticket;
import com.aeroreserve.repository.BookingRepository;
import com.aeroreserve.repository.UserRepository;

import java.util.List;

/** Coordinates seat allocation, payment, ticketing and cancellation in one transaction. */
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final FlightService flightService;
    private final SeatAllocationService seatService;
    private final PaymentService paymentService;

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository,
                          FlightService flightService, SeatAllocationService seatService,
                          PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.flightService = flightService;
        this.seatService = seatService;
        this.paymentService = paymentService;
    }

    /** Atomic from the user's perspective: partial failures are rolled back. */
    public synchronized BookingResult createBooking(String passengerId, String flightId,
                                                    CustomList<String> seatNumbers, PaymentMethod method)
            throws Exception {
        Passenger passenger = getPassenger(passengerId);
        Flight flight = flightService.getFlight(flightId);

        if (!flight.isBookable()) {
            throw new IllegalStateException("This flight is not available for booking");
        }
        if (seatNumbers.isEmpty()) {
            throw new InsufficientSeatsException("Select at least one seat");
        }
        if (flight.getAvailableSeatCount() < seatNumbers.size()) {
            throw new InsufficientSeatsException("Only " + flight.getAvailableSeatCount() + " seat(s) remain");
        }

        SeatClass seatClass = seatService.resolveSeatClass(flight, seatNumbers);
        double fare = seatService.calculateFare(flight, seatClass, seatNumbers.size());

        Booking booking = new Booking(passengerId, flightId, seatNumbers, seatClass, fare,
                passenger.getFullName(), passenger.getEmail(), flight.getFlightNumber());
        bookingRepository.put(booking.getBookingId(), booking);

        boolean seatsReserved = false;
        try {
            seatService.reserveSeats(flightId, seatNumbers, booking.getBookingId());
            seatsReserved = true;
            var payment = paymentService.process(booking.getBookingId(), booking.getTotalAmount(), method);
            booking.confirm(payment.getPaymentId());
            bookingRepository.put(booking.getBookingId(), booking);
            passenger.addBookingId(booking.getBookingId());
            userRepository.put(passengerId, passenger);
            return new BookingResult(booking, payment, buildTicket(booking.getBookingId()));
        } catch (Exception failure) {
            if (seatsReserved) {
                seatService.releaseSeats(flightId, seatNumbers, booking.getBookingId());
            }
            bookingRepository.remove(booking.getBookingId());
            throw failure;
        }
    }

    /** Passengers may cancel only their own bookings; admins may cancel any. */
    public synchronized Booking cancelBooking(String bookingId, String requestingUserId, boolean admin)
            throws Exception {
        Booking booking = getBooking(bookingId);
        if (!admin && !booking.belongsTo(requestingUserId)) {
            throw new UnauthorizedActionException("You cannot cancel another passenger's booking");
        }
        if (!booking.isActive()) {
            throw new IllegalStateException("This booking is already cancelled");
        }
        Flight flight = flightService.getFlight(booking.getFlightId());
        var policy = booking.getSeatClass() == SeatClass.FIRST
                ? new FlexibleCancellationPolicy() : new StandardCancellationPolicy();
        double fee = policy.calculateFee(booking, flight);
        double refund = Math.max(0.0, booking.getTotalAmount() - fee);

        seatService.releaseSeats(booking.getFlightId(), booking.getSeatNumbers(), bookingId);
        booking.cancel(fee, refund);
        bookingRepository.put(bookingId, booking);

        if (refund > 0) {
            paymentService.refund(bookingId, refund);
        }
        userRepository.findById(booking.getPassengerId()).ifPresent(user -> {
            if (user instanceof Passenger passenger) {
                passenger.removeBookingId(bookingId);
                userRepository.put(user.getUserId(), user);
            }
        });
        return booking;
    }

    public Ticket buildTicket(String bookingId) throws BookingNotFoundException, FlightNotFoundException {
        Booking booking = getBooking(bookingId);
        Flight flight = flightService.getFlight(booking.getFlightId());
        return new Ticket(booking.getTicketNumber(), booking.getBookingId(), booking.getPassengerName(),
                booking.getPassengerEmail(), flight.getFlightNumber(), flight.getDepartureAirport(),
                flight.getArrivalAirport(), flight.getDepartureDateTime(), flight.getArrivalDateTime(),
                booking.getSeatNumbers(), booking.getSeatClass(), booking.getBaseFare(),
                booking.getTaxesAndFees(), booking.getStatus());
    }

    public Booking getBooking(String bookingId) throws BookingNotFoundException {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
    }

    public List<Booking> findByPassenger(String passengerId) {
        return bookingRepository.findByPassenger(passengerId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    private Passenger getPassenger(String passengerId) throws UnauthorizedActionException {
        return userRepository.findById(passengerId)
                .filter(Passenger.class::isInstance)
                .map(Passenger.class::cast)
                .orElseThrow(() -> new UnauthorizedActionException("Passenger account not found"));
    }
}
