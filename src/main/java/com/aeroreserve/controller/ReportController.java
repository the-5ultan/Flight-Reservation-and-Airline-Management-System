package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.model.DashboardStatistics;
import com.aeroreserve.model.Flight;
import com.aeroreserve.ui.UiUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Data-driven reporting screen. */
public class ReportController {

    @FXML private Label revenueLabel;
    @FXML private Label refundLabel;
    @FXML private Label bookingMixLabel;
    @FXML private Label cancelLabel;
    @FXML private Label occupancyLabel;
    @FXML private Label seatsLabel;
    @FXML private StackPane statusChartHolder;
    @FXML private StackPane occupancyChartHolder;
    @FXML private TableView<Map.Entry<String, Integer>> routeTable;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> colRoute;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> colBookings;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> colFlights;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> colSeats;

    @FXML
    private void initialize() {
        colRoute.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        colBookings.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getValue())));
        colFlights.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(countFlights(data.getValue().getKey()))));
        colSeats.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(seatsSold(data.getValue().getKey()))));
        refresh();
    }

    private void refresh() {
        DashboardStatistics stats = ServiceContainer.get().reportService().computeDashboard();
        revenueLabel.setText(UiUtil.money(stats.getRevenue()));
        refundLabel.setText("Refunds issued: " + UiUtil.money(stats.getRefunds()));
        bookingMixLabel.setText(stats.getActiveBookings() + " active");
        cancelLabel.setText(stats.getCancelledBookings() + " cancelled · "
                + stats.getTotalBookings() + " total");
        occupancyLabel.setText(String.format("%.1f%%", stats.getOccupancyRate()));
        seatsLabel.setText(stats.getOccupiedSeats() + " occupied / " + stats.getAvailableSeats() + " free");

        var bookings = ServiceContainer.get().bookingService().getAllBookings();
        PieChart pie = new PieChart();
        pie.setData(javafx.collections.FXCollections.observableArrayList(
                new PieChart.Data("Confirmed", countStatus(bookings, BookingStatus.CONFIRMED)),
                new PieChart.Data("Completed", countStatus(bookings, BookingStatus.COMPLETED)),
                new PieChart.Data("Cancelled", countStatus(bookings, BookingStatus.CANCELLED)),
                new PieChart.Data("Pending", countStatus(bookings, BookingStatus.PENDING))));
        pie.setLabelsVisible(true);
        statusChartHolder.getChildren().setAll(pie);

        List<Flight> flights = ServiceContainer.get().flightService().getAllFlights();
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickLabelsVisible(false);
        NumberAxis yAxis = new NumberAxis(0, 100, 25);
        yAxis.setLabel("% occupied");
        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setLegendVisible(false);
        var series = new javafx.scene.chart.XYChart.Series<String, Number>();
        series.setName("Occupancy");
        flights.stream().limit(10).forEach(flight -> series.getData().add(
                new javafx.scene.chart.XYChart.Data<>(flight.getFlightNumber(), flight.getOccupancyRate())));
        bar.getData().add(series);
        occupancyChartHolder.getChildren().setAll(bar);

        routeTable.getItems().setAll(new ArrayList<>(stats.getPopularRoutes().entrySet()));
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    private long countStatus(List<com.aeroreserve.model.Booking> bookings, BookingStatus status) {
        return bookings.stream().filter(booking -> booking.getStatus() == status).count();
    }

    private long countFlights(String route) {
        return ServiceContainer.get().flightService().getAllFlights().stream()
                .filter(flight -> flight.getRoute().equals(route)).count();
    }

    private long seatsSold(String route) {
        return ServiceContainer.get().bookingService().getAllBookings().stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                .filter(booking -> {
                    try {
                        return ServiceContainer.get().flightService()
                                .getFlight(booking.getFlightId()).getRoute().equals(route);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .mapToInt(booking -> booking.getSeatNumbers().size())
                .sum();
    }
}
