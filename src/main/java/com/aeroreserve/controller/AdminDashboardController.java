package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.model.Booking;
import com.aeroreserve.model.DashboardStatistics;
import com.aeroreserve.model.Flight;
import com.aeroreserve.ui.UiUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

/** Live operations dashboard backed by ReportService. */
public class AdminDashboardController {

    @FXML private Label statFlights;
    @FXML private Label statPassengers;
    @FXML private Label statBookings;
    @FXML private Label statRevenue;
    @FXML private Label statBookingsHint;
    @FXML private Label statOccupancy;
    @FXML private Label seatBreakdown;
    @FXML private StackPane occupancyChartHolder;
    @FXML private StackPane routesChartHolder;
    @FXML private TableView<Booking> recentBookingsTable;
    @FXML private TableColumn<Booking, String> colBookingRef;
    @FXML private TableColumn<Booking, String> colBookingPassenger;
    @FXML private TableColumn<Booking, String> colBookingFlight;
    @FXML private TableColumn<Booking, String> colBookingAmount;
    @FXML private TableColumn<Booking, String> colBookingStatus;
    @FXML private TableView<Flight> upcomingFlightsTable;
    @FXML private TableColumn<Flight, String> colFlightNumber;
    @FXML private TableColumn<Flight, String> colFlightRoute;
    @FXML private TableColumn<Flight, String> colFlightDeparture;
    @FXML private TableColumn<Flight, String> colFlightSeats;

    @FXML
    private void initialize() {
        configureTables();
        refresh();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refresh();
    }

    private void refresh() {
        DashboardStatistics stats = ServiceContainer.get().reportService().computeDashboard();
        statFlights.setText(String.valueOf(stats.getTotalFlights()));
        statPassengers.setText(String.valueOf(stats.getTotalPassengers()));
        statBookings.setText(String.valueOf(stats.getTotalBookings()));
        statBookingsHint.setText(stats.getActiveBookings() + " active · " + stats.getCancelledBookings() + " cancelled");
        statRevenue.setText(UiUtil.money(stats.getRevenue()));
        statOccupancy.setText(String.format("%.1f%%", stats.getOccupancyRate()));
        seatBreakdown.setText(stats.getAvailableSeats() + " available · " + stats.getOccupiedSeats() + " occupied");

        PieChart pie = new PieChart();
        pie.setData(javafx.collections.FXCollections.observableArrayList(
                new PieChart.Data("Available", Math.max(1, stats.getAvailableSeats())),
                new PieChart.Data("Occupied", Math.max(1, stats.getOccupiedSeats()))));
        pie.setLegendVisible(true);
        pie.setLabelsVisible(false);
        occupancyChartHolder.getChildren().setAll(pie);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickUnit(1);
        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setLegendVisible(false);
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Bookings");
        stats.getPopularRoutes().forEach((route, count) ->
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(route, count)));
        bar.getData().add(series);
        routesChartHolder.getChildren().setAll(bar);

        recentBookingsTable.getItems().setAll(stats.getRecentBookings().toList());
        upcomingFlightsTable.getItems().setAll(stats.getUpcomingFlights().toList());
    }

    private void configureTables() {
        colBookingRef.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getBookingId()));
        colBookingPassenger.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPassengerName()));
        colBookingFlight.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFlightNumber()));
        colBookingAmount.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(UiUtil.money(data.getValue().getTotalAmount())));
        colBookingStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        colBookingStatus.setCellFactory(UiUtil.pillCell(value -> switch (value) {
            case "Confirmed" -> "pill-success";
            case "Cancelled" -> "pill-danger";
            case "Pending payment" -> "pill-warning";
            default -> "pill-info";
        }));

        colFlightNumber.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFlightNumber()));
        colFlightRoute.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRoute()));
        colFlightDeparture.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(UiUtil.dateTime(data.getValue().getDepartureDateTime())));
        colFlightSeats.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getAvailableSeatCount() + " / " + data.getValue().getCapacity()));
    }
}
