package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.model.Booking;
import com.aeroreserve.model.Ticket;
import com.aeroreserve.ui.DialogManager;
import com.aeroreserve.ui.SceneManager;
import com.aeroreserve.ui.UiUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Passenger ticket list with HTML preview, print and text export. */
public class TicketController {

    @FXML private ListView<Booking> ticketList;
    @FXML private TextArea ticketPreview;
    @FXML private Label previewTitle;
    @FXML private Button printButton;
    @FXML private Button exportButton;

    private Ticket currentTicket;

    @FXML
    private void initialize() {
        var user = ServiceContainer.get().auth().getCurrentUser();
        ticketList.setItems(FXCollections.observableArrayList(
                ServiceContainer.get().bookingService().findByPassenger(user.getUserId())));
        ticketList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Booking booking, boolean empty) {
                super.updateItem(booking, empty);
                if (empty || booking == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label label = new Label(booking.getFlightNumber() + "  ·  "
                            + booking.getBookingId() + "\n" + booking.getStatus().getDisplayName()
                            + "  ·  " + booking.getSeatList());
                    label.setWrapText(true);
                    label.getStyleClass().add("muted-text");
                    setGraphic(label);
                }
            }
        });
        ticketList.getSelectionModel().selectedItemProperty().addListener((obs, old, booking) -> {
            if (booking != null) {
                showTicket(booking);
            }
        });
        if (!ticketList.getItems().isEmpty()) {
            ticketList.getSelectionModel().selectFirst();
        } else {
            printButton.setDisable(true);
            exportButton.setDisable(true);
        }
    }

    private void showTicket(Booking booking) {
        try {
            currentTicket = ServiceContainer.get().bookingService().buildTicket(booking.getBookingId());
            ticketPreview.setText(currentTicket.toText());
            previewTitle.setText("Ticket " + currentTicket.getTicketNumber());
            printButton.setDisable(false);
            exportButton.setDisable(false);
        } catch (Exception e) {
            DialogManager.showError("Ticket", e.getMessage());
        }
    }

    @FXML
    private void handleExport(ActionEvent event) {
        if (currentTicket == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export ticket");
        chooser.setInitialFileName(currentTicket.getTicketNumber() + ".txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
        var file = chooser.showSaveDialog(ticketPreview.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), currentTicket.toText(), StandardCharsets.UTF_8);
            SceneManager.showToast("Ticket exported to " + file.getName(), true);
        } catch (IOException e) {
            DialogManager.showError("Export ticket", e.getMessage());
        }
    }

    @FXML
    private void handlePrint(ActionEvent event) {
        if (currentTicket == null) {
            return;
        }
        if (!UiUtil.printText(currentTicket.toText(), ticketPreview.getScene().getWindow())) {
            DialogManager.showInfo("Print", "Printing was cancelled or no printer is available.");
        }
    }
}
