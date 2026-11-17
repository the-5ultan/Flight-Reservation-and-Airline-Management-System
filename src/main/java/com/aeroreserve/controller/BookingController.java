package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.enums.UserRole;
import com.aeroreserve.model.Booking;
import com.aeroreserve.model.Ticket;
import com.aeroreserve.ui.DialogManager;
import com.aeroreserve.ui.SceneManager;
import com.aeroreserve.ui.UiUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Shared reservation screen with role-aware visibility and ticket export. */
public class BookingController {

    private static final String ALL = "All statuses";

    @FXML private Label pageTitle;
    @FXML private Label searchLabel;
    @FXML private Label summaryLabel;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, String> colRef;
    @FXML private TableColumn<Booking, String> colPassenger;
    @FXML private TableColumn<Booking, String> colFlight;
    @FXML private TableColumn<Booking, String> colRoute;
    @FXML private TableColumn<Booking, String> colSeats;
    @FXML private TableColumn<Booking, String> colClass;
    @FXML private TableColumn<Booking, String> colDate;
    @FXML private TableColumn<Booking, String> colAmount;
    @FXML private TableColumn<Booking, String> colStatus;
    @FXML private TableColumn<Booking, Void> colActions;
    @FXML private VBox previewPane;
    @FXML private TextArea ticketPreview;

    private FilteredList<Booking> filtered;
    private Ticket currentTicket;
    private boolean admin;

    @FXML
    private void initialize() {
        admin = ServiceContainer.get().auth().getCurrentUser() != null
                && ServiceContainer.get().auth().getCurrentUser().getRole() == UserRole.ADMIN;
        pageTitle.setText(admin ? "All bookings" : "My bookings");
        searchLabel.setVisible(admin);
        searchLabel.setManaged(admin);
        searchField.setVisible(admin);
        searchField.setManaged(admin);

        statusFilter.setItems(FXCollections.observableArrayList(ALL,
                BookingStatus.CONFIRMED.getDisplayName(), BookingStatus.PENDING.getDisplayName(),
                BookingStatus.CANCELLED.getDisplayName(), BookingStatus.COMPLETED.getDisplayName()));
        statusFilter.getSelectionModel().select(ALL);
        statusFilter.setConverter(new StringConverter<>() {
            @Override public String toString(String value) { return value == null ? "" : value; }
            @Override public String fromString(String text) { return text; }
        });
        statusFilter.valueProperty().addListener((obs, old, value) -> applyFilters());
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        configureColumns();
        loadBookings();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadBookings();
    }

    private void loadBookings() {
        var user = ServiceContainer.get().auth().getCurrentUser();
        List<Booking> source = admin
                ? ServiceContainer.get().bookingService().getAllBookings()
                : ServiceContainer.get().bookingService().findByPassenger(user.getUserId());
        filtered = new FilteredList<>(FXCollections.observableArrayList(source));
        bookingTable.setItems(filtered);
        applyFilters();
    }

    private void applyFilters() {
        if (filtered == null) {
            return;
        }
        String status = statusFilter.getValue();
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        filtered.setPredicate(booking -> {
            if (status != null && !ALL.equals(status)
                    && !booking.getStatus().getDisplayName().equals(status)) {
                return false;
            }
            if (query.isBlank()) {
                return true;
            }
            return booking.getBookingId().toLowerCase().contains(query)
                    || booking.getFlightNumber().toLowerCase().contains(query)
                    || (booking.getPassengerName() != null
                        && booking.getPassengerName().toLowerCase().contains(query))
                    || booking.getSeatList().toLowerCase().contains(query);
        });
        summaryLabel.setText(filtered.size() + " booking(s)");
    }

    private void configureColumns() {
        colRef.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBookingId()));
        colPassenger.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPassengerName()));
        colFlight.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFlightNumber()));
        colRoute.setCellValueFactory(data -> {
            try {
                var flight = ServiceContainer.get().flightService().getFlight(data.getValue().getFlightId());
                return new SimpleStringProperty(flight.getRoute());
            } catch (Exception e) {
                return new SimpleStringProperty("—");
            }
        });
        colSeats.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatList()));
        colClass.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeatClass().getDisplayName()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.dateTime(data.getValue().getBookingDate())));
        colAmount.setCellValueFactory(data -> new SimpleStringProperty(UiUtil.money(data.getValue().getTotalAmount())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(UiUtil.pillCell(value -> switch (value) {
            case "Confirmed" -> "pill-success";
            case "Cancelled" -> "pill-danger";
            case "Pending payment" -> "pill-warning";
            default -> "pill-info";
        }));
        colActions.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Booking booking = getTableView().getItems().get(getIndex());
                Button ticket = new Button("View ticket");
                ticket.getStyleClass().addAll("btn-small", "btn-secondary");
                ticket.setOnAction(e -> viewTicket(booking));
                Button cancel = new Button("Cancel");
                cancel.getStyleClass().addAll("btn-small", "btn-danger");
                cancel.setDisable(!booking.isActive());
                cancel.setOnAction(e -> cancelBooking(booking));
                HBox box = new HBox(6, ticket, cancel);
                box.setPadding(new Insets(3, 0, 3, 0));
                setGraphic(box);
            }
        });
    }

    private void viewTicket(Booking booking) {
        try {
            currentTicket = ServiceContainer.get().bookingService().buildTicket(booking.getBookingId());
            ticketPreview.setText(currentTicket.toText());
            previewPane.setVisible(true);
            previewPane.setManaged(true);
        } catch (Exception e) {
            DialogManager.showError("Ticket", e.getMessage());
        }
    }

    private void cancelBooking(Booking booking) {
        if (!DialogManager.confirm("Cancel booking", "Cancel booking " + booking.getBookingId()
                + "? Applicable charges will be deducted from the refund.")) {
            return;
        }
        try {
            var user = ServiceContainer.get().auth().getCurrentUser();
            ServiceContainer.get().bookingService().cancelBooking(booking.getBookingId(), user.getUserId(), admin);
            ServiceContainer.get().flushAll();
            loadBookings();
            SceneManager.showToast("Booking cancelled and seats released.", true);
        } catch (Exception e) {
            DialogManager.showError("Cancel booking", e.getMessage());
        }
    }

    @FXML
    private void handleClosePreview(ActionEvent event) {
        previewPane.setVisible(false);
        previewPane.setManaged(false);
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
