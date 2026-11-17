package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.enums.FlightStatus;
import com.aeroreserve.model.Aircraft;
import com.aeroreserve.model.Flight;
import com.aeroreserve.ui.DialogManager;
import com.aeroreserve.ui.SceneManager;
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
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Administrator screen for flight CRUD, scheduling and status changes. */
public class FlightManagementController {

    @FXML private VBox formPane;
    @FXML private Label formTitle;
    @FXML private Label formMessage;
    @FXML private Button toggleFormButton;
    @FXML private TextField flightNumberField;
    @FXML private ComboBox<Aircraft> aircraftCombo;
    @FXML private TextField fromField;
    @FXML private TextField toField;
    @FXML private DatePicker departureDate;
    @FXML private TextField departureTime;
    @FXML private DatePicker arrivalDate;
    @FXML private TextField arrivalTime;
    @FXML private TextField basePriceField;
    @FXML private ComboBox<FlightStatus> statusCombo;
    @FXML private Label countLabel;
    @FXML private TableView<Flight> flightTable;
    @FXML private TableColumn<Flight, String> colNumber;
    @FXML private TableColumn<Flight, String> colRoute;
    @FXML private TableColumn<Flight, String> colAircraft;
    @FXML private TableColumn<Flight, String> colDeparture;
    @FXML private TableColumn<Flight, String> colArrival;
    @FXML private TableColumn<Flight, String> colPrice;
    @FXML private TableColumn<Flight, String> colStatus;
    @FXML private TableColumn<Flight, String> colSeats;
    @FXML private TableColumn<Flight, Void> colActions;

    private String editingFlightId;

    @FXML
    private void initialize() {
        aircraftCombo.setItems(FXCollections.observableArrayList(
                ServiceContainer.get().aircraftService().getAllAircraft()));
        aircraftCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Aircraft aircraft) {
                return aircraft == null ? "" : aircraft.getModel() + " · " + aircraft.getRegistrationNumber();
            }
            @Override public Aircraft fromString(String text) {
                return aircraftCombo.getItems().stream()
                        .filter(a -> toString(a).equals(text)).findFirst().orElse(null);
            }
        });
        statusCombo.setItems(FXCollections.observableArrayList(FlightStatus.values()));
        statusCombo.setConverter(new StringConverter<>() {
            @Override public String toString(FlightStatus status) {
                return status == null ? "" : status.getDisplayName();
            }
            @Override public FlightStatus fromString(String text) { return null; }
        });
        statusCombo.getSelectionModel().select(FlightStatus.SCHEDULED);
        departureDate.setValue(java.time.LocalDate.now().plusDays(3));
        arrivalDate.setValue(java.time.LocalDate.now().plusDays(3));

        configureColumns();
        loadFlights();
    }

    @FXML
    private void handleToggleForm(ActionEvent event) {
        if (formPane.isVisible()) {
            hideForm();
        } else {
            resetForm();
            formPane.setVisible(true);
            formPane.setManaged(true);
            toggleFormButton.setText("×  Close form");
        }
    }

    @FXML
    private void handleCancelForm(ActionEvent event) {
        hideForm();
    }

    @FXML
    private void handleSave(ActionEvent event) {
        clearMessage();
        try {
            Aircraft aircraft = aircraftCombo.getValue();
            if (aircraft == null) {
                throw new IllegalArgumentException("Select an aircraft");
            }
            LocalDateTime departure = combine(departureDate.getValue(), departureTime.getText(), "departure");
            LocalDateTime arrival = combine(arrivalDate.getValue(), arrivalTime.getText(), "arrival");
            double price = Double.parseDouble(basePriceField.getText().trim());
            FlightStatus status = statusCombo.getValue();

            if (editingFlightId == null) {
                ServiceContainer.get().flightService().createFlight(flightNumberField.getText(),
                        aircraft.getAircraftId(), fromField.getText(), toField.getText(),
                        departure, arrival, price, status);
                showMessage("Flight created successfully.", true);
            } else {
                ServiceContainer.get().flightService().updateFlight(editingFlightId,
                        flightNumberField.getText(), fromField.getText(), toField.getText(),
                        departure, arrival, price, status);
                showMessage("Flight updated successfully.", true);
            }
            ServiceContainer.get().flushAll();
            loadFlights();
            hideForm();
        } catch (Exception e) {
            showMessage(e.getMessage(), false);
        }
    }

    private void edit(Flight flight) {
        editingFlightId = flight.getFlightId();
        flightNumberField.setText(flight.getFlightNumber());
        aircraftCombo.getSelectionModel().select(aircraftCombo.getItems().stream()
                .filter(a -> a.getAircraftId().equals(flight.getAircraftId()))
                .findFirst().orElse(null));
        fromField.setText(flight.getDepartureAirport());
        toField.setText(flight.getArrivalAirport());
        departureDate.setValue(flight.getDepartureDateTime().toLocalDate());
        departureTime.setText(flight.getDepartureDateTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        arrivalDate.setValue(flight.getArrivalDateTime().toLocalDate());
        arrivalTime.setText(flight.getArrivalDateTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        basePriceField.setText(String.format("%.2f", flight.getBasePrice()));
        statusCombo.getSelectionModel().select(flight.getStatus());
        formTitle.setText("Edit flight " + flight.getFlightNumber());
        formPane.setVisible(true);
        formPane.setManaged(true);
        toggleFormButton.setText("×  Close form");
    }

    private void cancelFlight(Flight flight) {
        if (!DialogManager.confirm("Cancel flight", "Cancel " + flight.getFlightNumber()
                + "? All active bookings will be cancelled and fully refunded.")) {
            return;
        }
        try {
            int affected = ServiceContainer.get().flightService().cancelFlight(flight.getFlightId());
            ServiceContainer.get().flushAll();
            loadFlights();
            SceneManager.showToast("Flight cancelled; " + affected + " booking(s) refunded.", true);
        } catch (Exception e) {
            DialogManager.showError("Cancel flight", e.getMessage());
        }
    }

    private void deleteFlight(Flight flight) {
        if (!DialogManager.confirm("Delete flight", "Permanently delete " + flight.getFlightNumber() + "?")) {
            return;
        }
        try {
            ServiceContainer.get().flightService().deleteFlight(flight.getFlightId());
            ServiceContainer.get().flushAll();
            loadFlights();
            SceneManager.showToast("Flight deleted.", true);
        } catch (Exception e) {
            DialogManager.showError("Delete flight", e.getMessage());
        }
    }

    private void loadFlights() {
        flightTable.getItems().setAll(ServiceContainer.get().flightService().getAllFlights());
        countLabel.setText(flightTable.getItems().size() + " flights");
    }

    private void configureColumns() {
        colNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFlightNumber()));
        colRoute.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoute()));
        colAircraft.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAircraftModel()));
        colDeparture.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.dateTime(data.getValue().getDepartureDateTime())));
        colArrival.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.dateTime(data.getValue().getArrivalDateTime())));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.money(data.getValue().getBasePrice())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(UiUtil.pillCell(value -> switch (value) {
            case "Cancelled" -> "pill-danger";
            case "Scheduled" -> "pill-success";
            case "Delayed" -> "pill-warning";
            default -> "pill-info";
        }));
        colSeats.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAvailableSeatCount() + "/" + data.getValue().getCapacity()));
        colActions.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Flight flight = getTableView().getItems().get(getIndex());
                Button edit = new Button("Edit");
                edit.getStyleClass().addAll("btn-small", "btn-ghost");
                edit.setOnAction(e -> edit(flight));
                Button cancel = new Button("Cancel flight");
                cancel.getStyleClass().addAll("btn-small", "btn-secondary");
                cancel.setDisable(flight.getStatus() == FlightStatus.CANCELLED);
                cancel.setOnAction(e -> cancelFlight(flight));
                Button delete = new Button("Delete");
                delete.getStyleClass().addAll("btn-small", "btn-danger");
                delete.setOnAction(e -> deleteFlight(flight));
                HBox box = new HBox(6, edit, cancel, delete);
                box.setPadding(new Insets(3, 0, 3, 0));
                setGraphic(box);
            }
        });
    }

    private static LocalDateTime combine(java.time.LocalDate date, String time, String label) {
        if (date == null) {
            throw new IllegalArgumentException("Select a " + label + " date");
        }
        try {
            return LocalDateTime.of(date, LocalTime.parse(time.trim(), DateTimeFormatter.ofPattern("HH:mm")));
        } catch (Exception e) {
            throw new IllegalArgumentException("Enter " + label + " time as HH:mm");
        }
    }

    private void resetForm() {
        editingFlightId = null;
        formTitle.setText("New flight");
        flightNumberField.clear();
        aircraftCombo.getSelectionModel().selectFirst();
        fromField.clear();
        toField.clear();
        departureDate.setValue(java.time.LocalDate.now().plusDays(3));
        departureTime.setText("09:00");
        arrivalDate.setValue(java.time.LocalDate.now().plusDays(3));
        arrivalTime.setText("12:00");
        basePriceField.setText("250");
        statusCombo.getSelectionModel().select(FlightStatus.SCHEDULED);
        clearMessage();
    }

    private void hideForm() {
        formPane.setVisible(false);
        formPane.setManaged(false);
        toggleFormButton.setText("+  New flight");
        editingFlightId = null;
    }

    private void showMessage(String text, boolean success) {
        formMessage.setText(text);
        formMessage.setVisible(true);
        formMessage.setManaged(true);
        formMessage.getStyleClass().remove("success");
        if (success) {
            formMessage.getStyleClass().add("success");
        }
    }

    private void clearMessage() {
        formMessage.setText("");
        formMessage.setVisible(false);
        formMessage.setManaged(false);
    }
}
