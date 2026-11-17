package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.FlightSearchCriteria;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.ui.DialogManager;
import com.aeroreserve.ui.SceneManager;
import com.aeroreserve.ui.UiUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

/** Passenger home screen with live personal figures and featured flights. */
public class PassengerDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statBookings;
    @FXML private Label statUpcoming;
    @FXML private Label statSeats;
    @FXML private Label statSpent;
    @FXML private TableView<Flight> featuredTable;
    @FXML private TableColumn<Flight, String> colFlight;
    @FXML private TableColumn<Flight, String> colRoute;
    @FXML private TableColumn<Flight, String> colDeparture;
    @FXML private TableColumn<Flight, String> colFare;
    @FXML private TableColumn<Flight, String> colSeats;
    @FXML private TableColumn<Flight, Void> colActions;

    @FXML
    private void initialize() {
        Passenger passenger = (Passenger) ServiceContainer.get().auth().getCurrentUser();
        welcomeLabel.setText("Hello, " + passenger.getFullName().split("\\s+")[0]);
        var bookings = ServiceContainer.get().bookingService().findByPassenger(passenger.getUserId());
        long upcoming = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        int seats = bookings.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .mapToInt(b -> b.getSeatNumbers().size()).sum();
        double spent = ServiceContainer.get().paymentService().getAllPayments().stream()
                .filter(payment -> bookings.stream().anyMatch(b -> b.getBookingId().equals(payment.getBookingId())))
                .mapToDouble(payment -> payment.getAmount() - payment.getRefundedAmount())
                .sum();

        statBookings.setText(String.valueOf(bookings.size()));
        statUpcoming.setText(String.valueOf(upcoming));
        statSeats.setText(String.valueOf(seats));
        statSpent.setText(UiUtil.money(spent));

        configureColumns();
        featuredTable.getItems().setAll(ServiceContainer.get().flightService()
                .searchFlights(new FlightSearchCriteria()).stream().limit(6).toList());
    }

    @FXML private void handleSearch(ActionEvent event) { SceneManager.navigate("search"); }
    @FXML private void handleBookings(ActionEvent event) { SceneManager.navigate("bookings"); }

    private void configureColumns() {
        colFlight.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFlightNumber()));
        colRoute.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoute()));
        colDeparture.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.dateTime(data.getValue().getDepartureDateTime())));
        colFare.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.money(data.getValue().getBasePrice())));
        colSeats.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getAvailableSeatCount())));
        colActions.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Flight flight = getTableView().getItems().get(getIndex());
                Button button = new Button("Select seats  →");
                button.getStyleClass().addAll("btn-small", "btn-primary");
                button.setOnAction(e -> DialogManager.openModal("/fxml/seat-selection.fxml",
                        "Seat selection · " + flight.getFlightNumber(), 1100, 760,
                        controller -> ((SeatSelectionController) controller).setFlight(flight)));
                HBox box = new HBox(button);
                box.setPadding(new Insets(3, 0, 3, 0));
                setGraphic(box);
            }
        });
    }
}
