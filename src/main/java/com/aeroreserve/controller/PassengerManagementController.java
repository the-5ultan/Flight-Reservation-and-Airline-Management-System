package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.ui.DialogManager;
import com.aeroreserve.ui.SceneManager;
import com.aeroreserve.ui.UiUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

/** Administrator view of registered passengers. */
public class PassengerManagementController {

    @FXML private Label countLabel;
    @FXML private TableView<Passenger> passengerTable;
    @FXML private TableColumn<Passenger, String> colName;
    @FXML private TableColumn<Passenger, String> colEmail;
    @FXML private TableColumn<Passenger, String> colPhone;
    @FXML private TableColumn<Passenger, String> colNationality;
    @FXML private TableColumn<Passenger, String> colPassport;
    @FXML private TableColumn<Passenger, String> colBookings;
    @FXML private TableColumn<Passenger, String> colJoined;
    @FXML private TableColumn<Passenger, String> colStatus;
    @FXML private TableColumn<Passenger, Void> colActions;

    @FXML
    private void initialize() {
        configureColumns();
        loadPassengers();
    }

    private void loadPassengers() {
        var passengers = ServiceContainer.get().repositories().users().findAllPassengers();
        passengerTable.getItems().setAll(passengers);
        countLabel.setText(passengers.size() + " registered passenger(s)");
    }

    private void configureColumns() {
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        colNationality.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getNationality().isEmpty() ? "—" : data.getValue().getNationality()));
        colPassport.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPassportNumber().isEmpty() ? "—" : data.getValue().getPassportNumber()));
        colBookings.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getBookingIds().size())));
        colJoined.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.dateTime(data.getValue().getCreatedAt())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isActive() ? "Active" : "Deactivated"));
        colStatus.setCellFactory(UiUtil.pillCell(value -> "Active".equals(value) ? "pill-success" : "pill-danger"));
        colActions.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Passenger passenger = getTableView().getItems().get(getIndex());
                Button toggle = new Button(passenger.isActive() ? "Deactivate" : "Activate");
                toggle.getStyleClass().addAll("btn-small",
                        passenger.isActive() ? "btn-danger" : "btn-secondary");
                toggle.setOnAction(e -> {
                    String action = passenger.isActive() ? "deactivate" : "activate";
                    if (!DialogManager.confirm("Confirm", "Do you want to " + action + " this account?")) {
                        return;
                    }
                    passenger.setActive(!passenger.isActive());
                    ServiceContainer.get().repositories().users()
                            .put(passenger.getUserId(), passenger);
                    ServiceContainer.get().flushAll();
                    loadPassengers();
                    SceneManager.showToast("Account " + action + "d.", true);
                });
                HBox box = new HBox(6, toggle);
                box.setPadding(new Insets(3, 0, 3, 0));
                setGraphic(box);
            }
        });
    }
}
