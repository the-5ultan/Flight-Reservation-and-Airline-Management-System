package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.model.Aircraft;
import com.aeroreserve.ui.DialogManager;
import com.aeroreserve.ui.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Administrator screen for aircraft fleet and seat configuration. */
public class AircraftManagementController {

    @FXML private VBox formPane;
    @FXML private Label formTitle;
    @FXML private Label formMessage;
    @FXML private Label firstCapacityLabel;
    @FXML private Label businessCapacityLabel;
    @FXML private Label economyCapacityLabel;
    @FXML private Button toggleFormButton;
    @FXML private TextField modelField;
    @FXML private TextField manufacturerField;
    @FXML private TextField registrationField;
    @FXML private TextField firstRowsField;
    @FXML private TextField firstColumnsField;
    @FXML private TextField businessRowsField;
    @FXML private TextField businessColumnsField;
    @FXML private TextField economyRowsField;
    @FXML private TextField economyColumnsField;
    @FXML private TableView<Aircraft> aircraftTable;
    @FXML private TableColumn<Aircraft, String> colModel;
    @FXML private TableColumn<Aircraft, String> colManufacturer;
    @FXML private TableColumn<Aircraft, String> colRegistration;
    @FXML private TableColumn<Aircraft, String> colFirst;
    @FXML private TableColumn<Aircraft, String> colBusiness;
    @FXML private TableColumn<Aircraft, String> colEconomy;
    @FXML private TableColumn<Aircraft, String> colTotal;
    @FXML private TableColumn<Aircraft, Void> colActions;

    private String editingId;

    @FXML
    private void initialize() {
        configureColumns();
        loadFleet();
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
            int firstRows = Integer.parseInt(firstRowsField.getText().trim());
            int businessRows = Integer.parseInt(businessRowsField.getText().trim());
            int economyRows = Integer.parseInt(economyRowsField.getText().trim());
            if (editingId == null) {
                ServiceContainer.get().aircraftService().addAircraft(modelField.getText(),
                        manufacturerField.getText(), registrationField.getText(),
                        firstRows, firstColumnsField.getText(), businessRows, businessColumnsField.getText(),
                        economyRows, economyColumnsField.getText());
                showMessage("Aircraft added with " + previewCapacity() + " generated seats.", true);
            } else {
                ServiceContainer.get().aircraftService().updateAircraft(editingId, modelField.getText(),
                        manufacturerField.getText(), registrationField.getText(),
                        firstRows, firstColumnsField.getText(), businessRows, businessColumnsField.getText(),
                        economyRows, economyColumnsField.getText());
                showMessage("Aircraft updated and seat map regenerated.", true);
            }
            ServiceContainer.get().flushAll();
            loadFleet();
        } catch (Exception e) {
            showMessage(e.getMessage(), false);
        }
    }

    private void edit(Aircraft aircraft) {
        editingId = aircraft.getAircraftId();
        modelField.setText(aircraft.getModel());
        manufacturerField.setText(aircraft.getManufacturer());
        registrationField.setText(aircraft.getRegistrationNumber());
        firstRowsField.setText(String.valueOf(aircraft.getFirstRows()));
        firstColumnsField.setText(aircraft.getFirstColumns());
        businessRowsField.setText(String.valueOf(aircraft.getBusinessRows()));
        businessColumnsField.setText(aircraft.getBusinessColumns());
        economyRowsField.setText(String.valueOf(aircraft.getEconomyRows()));
        economyColumnsField.setText(aircraft.getEconomyColumns());
        updateCapacityPreview();
        formTitle.setText("Edit " + aircraft.getModel());
        formPane.setVisible(true);
        formPane.setManaged(true);
        toggleFormButton.setText("×  Close form");
    }

    private void delete(Aircraft aircraft) {
        if (!DialogManager.confirm("Delete aircraft", "Remove " + aircraft.getModel() + " from the fleet?")) {
            return;
        }
        try {
            ServiceContainer.get().aircraftService().deleteAircraft(aircraft.getAircraftId());
            ServiceContainer.get().flushAll();
            loadFleet();
            SceneManager.showToast("Aircraft removed.", true);
        } catch (Exception e) {
            DialogManager.showError("Delete aircraft", e.getMessage());
        }
    }

    private void loadFleet() {
        aircraftTable.getItems().setAll(ServiceContainer.get().aircraftService().getAllAircraft());
    }

    private void configureColumns() {
        colModel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getModel()));
        colManufacturer.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getManufacturer()));
        colRegistration.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRegistrationNumber()));
        colFirst.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getFirstCapacity())));
        colBusiness.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getBusinessCapacity())));
        colEconomy.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getEconomyCapacity())));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalCapacity())));
        colActions.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Aircraft aircraft = getTableView().getItems().get(getIndex());
                Button edit = new Button("Edit");
                edit.getStyleClass().addAll("btn-small", "btn-ghost");
                edit.setOnAction(e -> edit(aircraft));
                Button delete = new Button("Delete");
                delete.getStyleClass().addAll("btn-small", "btn-danger");
                delete.setOnAction(e -> delete(aircraft));
                HBox box = new HBox(6, edit, delete);
                box.setPadding(new Insets(3, 0, 3, 0));
                setGraphic(box);
            }
        });
    }

    private int previewCapacity() {
        try {
            return Integer.parseInt(firstRowsField.getText()) * firstColumnsField.getText().length()
                    + Integer.parseInt(businessRowsField.getText()) * businessColumnsField.getText().length()
                    + Integer.parseInt(economyRowsField.getText()) * economyColumnsField.getText().length();
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateCapacityPreview() {
        firstCapacityLabel.setText(parse(firstRowsField) * firstColumnsField.getText().length() + " seats");
        businessCapacityLabel.setText(parse(businessRowsField) * businessColumnsField.getText().length() + " seats");
        economyCapacityLabel.setText(parse(economyRowsField) * economyColumnsField.getText().length() + " seats");
    }

    private static int parse(TextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void resetForm() {
        editingId = null;
        formTitle.setText("New aircraft");
        modelField.clear();
        manufacturerField.clear();
        registrationField.clear();
        firstRowsField.setText("0");
        firstColumnsField.setText("AF");
        businessRowsField.setText("2");
        businessColumnsField.setText("ACDF");
        economyRowsField.setText("12");
        economyColumnsField.setText("ABCDEF");
        updateCapacityPreview();
        clearMessage();
    }

    private void hideForm() {
        formPane.setVisible(false);
        formPane.setManaged(false);
        toggleFormButton.setText("+  New aircraft");
        editingId = null;
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
