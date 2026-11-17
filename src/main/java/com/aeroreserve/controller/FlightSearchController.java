package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.enums.FlightStatus;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.FlightSearchCriteria;
import com.aeroreserve.ui.DialogManager;
import com.aeroreserve.ui.UiUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

/** Passenger flight search with filtering, sorting and seat-selection entry point. */
public class FlightSearchController {

    @FXML private TextField fromField;
    @FXML private TextField toField;
    @FXML private DatePicker travelDate;
    @FXML private ComboBox<Integer> passengerCount;
    @FXML private TextField maxPriceField;
    @FXML private ComboBox<SortOption> sortCombo;
    @FXML private TextField minSeatsField;
    @FXML private ComboBox<FlightStatus> statusCombo;
    @FXML private Label resultCount;
    @FXML private TableView<Flight> resultsTable;
    @FXML private TableColumn<Flight, String> colFlight;
    @FXML private TableColumn<Flight, String> colRoute;
    @FXML private TableColumn<Flight, String> colDeparture;
    @FXML private TableColumn<Flight, String> colArrival;
    @FXML private TableColumn<Flight, String> colAircraft;
    @FXML private TableColumn<Flight, String> colFare;
    @FXML private TableColumn<Flight, String> colSeats;
    @FXML private TableColumn<Flight, String> colStatus;
    @FXML private TableColumn<Flight, Void> colActions;

    private enum SortOption {
        DEPARTURE_ASC("Departure · earliest"),
        DEPARTURE_DESC("Departure · latest"),
        PRICE_ASC("Fare · lowest first"),
        PRICE_DESC("Fare · highest first"),
        SEATS_ASC("Seats · fewest first");

        private final String label;

        SortOption(String label) {
            this.label = label;
        }
    }

    @FXML
    private void initialize() {
        passengerCount.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6));
        passengerCount.getSelectionModel().select(1);
        sortCombo.setItems(FXCollections.observableArrayList(SortOption.values()));
        sortCombo.getSelectionModel().select(SortOption.DEPARTURE_ASC);
        sortCombo.setConverter(new StringConverter<>() {
            @Override public String toString(SortOption option) { return option == null ? "" : option.label; }
            @Override public SortOption fromString(String text) { return null; }
        });
        statusCombo.setItems(FXCollections.observableArrayList(FlightStatus.SCHEDULED, FlightStatus.DELAYED));
        statusCombo.setConverter(new StringConverter<>() {
            @Override public String toString(FlightStatus status) { return status == null ? "Any" : status.getDisplayName(); }
            @Override public FlightStatus fromString(String text) { return null; }
        });
        configureColumns();
        handleSearch(null);
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        FlightSearchCriteria criteria = new FlightSearchCriteria(fromField.getText(), toField.getText(),
                travelDate.getValue(), passengerCount.getValue() == null ? 1 : passengerCount.getValue());
        criteria.setMaxPrice(parseDouble(maxPriceField.getText()));
        criteria.setMinAvailableSeats(parseInt(minSeatsField.getText()));
        criteria.setStatus(statusCombo.getValue());
        criteria.setSortBy(FlightSearchCriteria.SortOption.valueOf(sortCombo.getValue().name()));

        var results = ServiceContainer.get().flightService().searchFlights(criteria);
        resultsTable.getItems().setAll(results);
        resultCount.setText(results.size() + " result" + (results.size() == 1 ? "" : "s"));
    }

    @FXML
    private void handleClear(ActionEvent event) {
        fromField.clear();
        toField.clear();
        travelDate.setValue(null);
        passengerCount.getSelectionModel().select(1);
        maxPriceField.clear();
        minSeatsField.clear();
        statusCombo.getSelectionModel().select(null);
        sortCombo.getSelectionModel().select(SortOption.DEPARTURE_ASC);
        handleSearch(null);
    }

    private void configureColumns() {
        colFlight.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFlightNumber()));
        colRoute.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoute()));
        colDeparture.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.dateTime(data.getValue().getDepartureDateTime())));
        colArrival.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.dateTime(data.getValue().getArrivalDateTime())));
        colAircraft.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAircraftModel()));
        colFare.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.money(data.getValue().getBasePrice())));
        colSeats.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getAvailableSeatCount())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(UiUtil.pillCell(value -> "pill-success"));
        colActions.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Flight flight = getTableView().getItems().get(getIndex());
                Button select = new Button("Select seats  →");
                select.getStyleClass().addAll("btn-small", "btn-primary");
                select.setOnAction(e -> openSeatSelection(flight));
                HBox box = new HBox(select);
                box.setPadding(new Insets(3, 0, 3, 0));
                setGraphic(box);
            }
        });
    }

    private void openSeatSelection(Flight flight) {
        var modal = DialogManager.openModal("/fxml/seat-selection.fxml", "Seat selection · " + flight.getFlightNumber(),
                1100, 760, controller -> ((SeatSelectionController) controller).setFlight(flight));
        modal.setOnHidden(event -> handleSearch(null));
    }

    private static Double parseDouble(String text) {
        try {
            return text == null || text.isBlank() ? null : Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String text) {
        try {
            return text == null || text.isBlank() ? null : Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
