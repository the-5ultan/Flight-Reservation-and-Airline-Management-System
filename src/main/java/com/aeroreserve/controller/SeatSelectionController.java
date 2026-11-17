package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.enums.PaymentType;
import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.enums.SeatPosition;
import com.aeroreserve.enums.SeatStatus;
import com.aeroreserve.generic.CustomList;
import com.aeroreserve.model.CardPayment;
import com.aeroreserve.model.CashPayment;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.model.PaymentMethod;
import com.aeroreserve.model.Seat;
import com.aeroreserve.ui.DialogManager;
import com.aeroreserve.ui.SceneManager;
import com.aeroreserve.ui.UiUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Visual aircraft seat map with guarded allocation and simulated payment. */
public class SeatSelectionController {

    @FXML private Label flightInfo;
    @FXML private StackPane seatGridHolder;
    @FXML private Label selectedSeatsLabel;
    @FXML private Label cabinLabel;
    @FXML private Label farePerSeatLabel;
    @FXML private Label totalLabel;
    @FXML private Label messageLabel;
    @FXML private ComboBox<String> paymentCombo;
    @FXML private VBox cardPane;
    @FXML private VBox cashPane;
    @FXML private javafx.scene.control.TextField cardHolderField;
    @FXML private javafx.scene.control.TextField cardNumberField;
    @FXML private javafx.scene.control.TextField cardExpiryField;
    @FXML private javafx.scene.control.TextField cashCounterField;

    private static final int MAX_SEATS = 6;

    private Flight flight;
    private final Set<String> selectedSeats = new LinkedHashSet<>();
    private final Map<String, Label> seatLabels = new LinkedHashMap<>();

    @FXML
    private void initialize() {
        paymentCombo.setItems(FXCollections.observableArrayList("Card", "Cash"));
        paymentCombo.getSelectionModel().select("Card");
        paymentCombo.valueProperty().addListener((obs, old, value) -> {
            boolean card = "Card".equals(value);
            cardPane.setVisible(card);
            cardPane.setManaged(card);
            cashPane.setVisible(!card);
            cashPane.setManaged(!card);
        });
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
        flightInfo.setText(flight.getFlightNumber() + "  ·  " + flight.getRoute()
                + "  ·  " + UiUtil.dateTime(flight.getDepartureDateTime())
                + "  ·  " + flight.getAircraftModel());
        buildSeatMap();
        updateSummary();
    }

    private void buildSeatMap() {
        selectedSeats.clear();
        seatLabels.clear();
        Map<Integer, List<Seat>> byRow = new LinkedHashMap<>();
        for (Seat seat : flight.getSeatsSortedForDisplay()) {
            byRow.computeIfAbsent(seat.getRow(), key -> new ArrayList<>()).add(seat);
        }

        VBox rows = new VBox(7);
        rows.setAlignment(Pos.CENTER);
        for (Map.Entry<Integer, List<Seat>> entry : byRow.entrySet()) {
            List<Seat> seats = entry.getValue();
            seats.sort((a, b) -> a.getColumn().compareTo(b.getColumn()));
            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER);
            Label rowNumber = new Label(String.format("%02d", entry.getKey()));
            rowNumber.getStyleClass().add("overline");
            rowNumber.setMinWidth(28);
            row.getChildren().add(rowNumber);
            for (int i = 0; i < seats.size(); i++) {
                Seat seat = seats.get(i);
                if (i > 0 && seats.get(i - 1).getPosition() == SeatPosition.AISLE
                        && seat.getPosition() == SeatPosition.AISLE) {
                    Pane aisle = new Pane();
                    aisle.setMinWidth(22);
                    aisle.setPrefWidth(22);
                    row.getChildren().add(aisle);
                }
                Label seatLabel = createSeatLabel(seat);
                row.getChildren().add(seatLabel);
            }
            rows.getChildren().add(row);
        }
        seatGridHolder.getChildren().setAll(rows);
    }

    private Label createSeatLabel(Seat seat) {
        Label label = new Label(seat.getSeatNumber());
        label.getStyleClass().addAll("seat", styleFor(seat.getSeatClass()));
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            label.getStyleClass().add("seat-occupied");
        }
        String availability = seat.isAvailable() ? "Available" : seat.getStatus().getDisplayName();
        label.setTooltip(new Tooltip(seat.getSeatNumber() + "  ·  " + seat.getSeatClass().getDisplayName()
                + "  ·  " + seat.getPosition().getDisplayName() + "  ·  " + availability));
        seatLabels.put(seat.getSeatNumber(), label);
        label.setOnMouseClicked(event -> toggleSeat(seat, label));
        return label;
    }

    private static String styleFor(SeatClass seatClass) {
        return switch (seatClass) {
            case FIRST -> "seat-first";
            case BUSINESS -> "seat-business";
            case ECONOMY -> "seat-economy";
        };
    }

    private void toggleSeat(Seat seat, Label label) {
        if (!seat.isAvailable()) {
            SceneManager.showToast("Seat " + seat.getSeatNumber() + " is already occupied.", false);
            return;
        }
        if (selectedSeats.contains(seat.getSeatNumber())) {
            selectedSeats.remove(seat.getSeatNumber());
            label.getStyleClass().remove("seat-selected");
        } else {
            if (selectedSeats.size() >= MAX_SEATS) {
                SceneManager.showToast("You can select up to " + MAX_SEATS + " seats per booking.", false);
                return;
            }
            if (!selectedSeats.isEmpty()) {
                SeatClass current = flight.getSeat(selectedSeats.iterator().next()).getSeatClass();
                if (current != seat.getSeatClass()) {
                    SceneManager.showToast("Seats in one booking must be in the same cabin.", false);
                    return;
                }
            }
            selectedSeats.add(seat.getSeatNumber());
            label.getStyleClass().add("seat-selected");
        }
        updateSummary();
    }

    private void updateSummary() {
        selectedSeatsLabel.setText(selectedSeats.isEmpty() ? "None" : String.join(", ", selectedSeats));
        if (selectedSeats.isEmpty()) {
            cabinLabel.setText("—");
            farePerSeatLabel.setText("$0.00");
            totalLabel.setText("$0.00");
            return;
        }
        SeatClass seatClass = flight.getSeat(selectedSeats.iterator().next()).getSeatClass();
        double perSeat = flight.getFare(seatClass);
        double subtotal = perSeat * selectedSeats.size();
        double taxes = Math.round((subtotal * 0.075 + 12.0) * 100.0) / 100.0;
        cabinLabel.setText(seatClass.getDisplayName());
        farePerSeatLabel.setText(UiUtil.money(perSeat));
        totalLabel.setText(UiUtil.money(subtotal + taxes));
    }

    @FXML
    private void handleClose(ActionEvent event) {
        closeWindow();
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        clearMessage();
        if (selectedSeats.isEmpty()) {
            showMessage("Select at least one available seat.", false);
            return;
        }
        try {
            ServiceContainer.get().auth().requirePassenger();
            var currentUser = ServiceContainer.get().auth().getCurrentUser();
            if (!(currentUser instanceof Passenger passenger)) {
                throw new IllegalStateException("Only passengers can create bookings");
            }
            PaymentMethod method = buildPaymentMethod();
            CustomList<String> seats = new CustomList<>(selectedSeats);
            var result = ServiceContainer.get().bookingService().createBooking(
                    passenger.getUserId(), flight.getFlightId(), seats, method);
            ServiceContainer.get().flushAll();
            DialogManager.showInfo("Booking confirmed", "Your booking reference is "
                    + result.getBooking().getBookingId() + "\n\n" + result.getTicket().toText());
            closeWindow();
        } catch (Exception e) {
            showMessage(e.getMessage(), false);
        }
    }

    private PaymentMethod buildPaymentMethod() throws Exception {
        if ("Card".equals(paymentCombo.getValue())) {
            String expiryText = cardExpiryField.getText().trim();
            if (!expiryText.matches("\\d{2}/\\d{2}")) {
                throw new IllegalArgumentException("Enter card expiry as MM/YY");
            }
            int month = Integer.parseInt(expiryText.substring(0, 2));
            int year = 2000 + Integer.parseInt(expiryText.substring(3));
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Enter a valid expiry month");
            }
            return new CardPayment(cardHolderField.getText(), cardNumberField.getText(),
                    YearMonth.of(year, month));
        }
        return new CashPayment(cashCounterField.getText().isBlank()
                ? "Main ticket counter" : cashCounterField.getText());
    }

    private void closeWindow() {
        if (seatGridHolder.getScene() != null) {
            ((Stage) seatGridHolder.getScene().getWindow()).close();
        }
    }

    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.getStyleClass().remove("success");
        if (success) {
            messageLabel.getStyleClass().add("success");
        }
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }
}
