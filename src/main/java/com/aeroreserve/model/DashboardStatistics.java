package com.aeroreserve.model;

import com.aeroreserve.generic.CustomList;

import java.util.LinkedHashMap;
import java.util.Map;

/** Snapshot of live figures calculated from persisted application data. */
public class DashboardStatistics {

    private final int totalFlights;
    private final int totalPassengers;
    private final int totalBookings;
    private final int activeBookings;
    private final int cancelledBookings;
    private final int availableSeats;
    private final int occupiedSeats;
    private final double revenue;
    private final double refunds;
    private final double occupancyRate;
    private final CustomList<Booking> recentBookings;
    private final CustomList<Flight> upcomingFlights;
    private final Map<String, Integer> popularRoutes;

    public DashboardStatistics(int totalFlights, int totalPassengers, int totalBookings,
                               int activeBookings, int cancelledBookings, int availableSeats,
                               int occupiedSeats, double revenue, double refunds, double occupancyRate,
                               CustomList<Booking> recentBookings, CustomList<Flight> upcomingFlights,
                               Map<String, Integer> popularRoutes) {
        this.totalFlights = totalFlights;
        this.totalPassengers = totalPassengers;
        this.totalBookings = totalBookings;
        this.activeBookings = activeBookings;
        this.cancelledBookings = cancelledBookings;
        this.availableSeats = availableSeats;
        this.occupiedSeats = occupiedSeats;
        this.revenue = revenue;
        this.refunds = refunds;
        this.occupancyRate = occupancyRate;
        this.recentBookings = recentBookings;
        this.upcomingFlights = upcomingFlights;
        this.popularRoutes = new LinkedHashMap<>(popularRoutes);
    }

    public int getTotalFlights() {
        return totalFlights;
    }

    public int getTotalPassengers() {
        return totalPassengers;
    }

    public int getTotalBookings() {
        return totalBookings;
    }

    public int getActiveBookings() {
        return activeBookings;
    }

    public int getCancelledBookings() {
        return cancelledBookings;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public int getOccupiedSeats() {
        return occupiedSeats;
    }

    public double getRevenue() {
        return revenue;
    }

    public double getRefunds() {
        return refunds;
    }

    public double getOccupancyRate() {
        return occupancyRate;
    }

    public CustomList<Booking> getRecentBookings() {
        return recentBookings;
    }

    public CustomList<Flight> getUpcomingFlights() {
        return upcomingFlights;
    }

    public Map<String, Integer> getPopularRoutes() {
        return popularRoutes;
    }
}
