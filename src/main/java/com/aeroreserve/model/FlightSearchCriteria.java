package com.aeroreserve.model;

import com.aeroreserve.enums.FlightStatus;

import java.time.LocalDate;

/** Value object carrying all search, filter and sort options for flight results. */
public class FlightSearchCriteria {

    public enum SortOption {
        DEPARTURE_ASC, DEPARTURE_DESC, PRICE_ASC, PRICE_DESC, SEATS_ASC
    }

    private String departureAirport;
    private String arrivalAirport;
    private LocalDate travelDate;
    private int passengerCount = 1;
    private Double maxPrice;
    private Integer minAvailableSeats;
    private FlightStatus status;
    private SortOption sortBy = SortOption.DEPARTURE_ASC;

    public FlightSearchCriteria() {
    }

    public FlightSearchCriteria(String departureAirport, String arrivalAirport, LocalDate travelDate,
                                int passengerCount) {
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.travelDate = travelDate;
        this.passengerCount = Math.max(1, passengerCount);
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(String departureAirport) {
        this.departureAirport = departureAirport;
    }

    public String getArrivalAirport() {
        return arrivalAirport;
    }

    public void setArrivalAirport(String arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = Math.max(1, passengerCount);
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getMinAvailableSeats() {
        return minAvailableSeats;
    }

    public void setMinAvailableSeats(Integer minAvailableSeats) {
        this.minAvailableSeats = minAvailableSeats;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public void setStatus(FlightStatus status) {
        this.status = status;
    }

    public SortOption getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortOption sortBy) {
        this.sortBy = sortBy == null ? SortOption.DEPARTURE_ASC : sortBy;
    }

    public boolean matchesRoute(Flight flight) {
        if (departureAirport != null && !departureAirport.isBlank()
                && !flight.getDepartureAirport().equalsIgnoreCase(departureAirport.trim())) {
            return false;
        }
        return arrivalAirport == null || arrivalAirport.isBlank()
                || flight.getArrivalAirport().equalsIgnoreCase(arrivalAirport.trim());
    }
}
