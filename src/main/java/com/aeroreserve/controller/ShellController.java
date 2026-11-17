package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.enums.UserRole;
import com.aeroreserve.model.User;
import com.aeroreserve.ui.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/** Application shell: role-aware navigation, header and screen switching. */
public class ShellController {

    @FXML private VBox adminNav;
    @FXML private VBox passengerNav;
    @FXML private Button navDashboard;
    @FXML private Button navFlights;
    @FXML private Button navAircraft;
    @FXML private Button navBookingsAdmin;
    @FXML private Button navPassengers;
    @FXML private Button navReports;
    @FXML private Button navSearch;
    @FXML private Button navBookings;
    @FXML private Button navTickets;
    @FXML private Button navProfile;
    @FXML private Button logoutButton;
    @FXML private Label headerTitle;
    @FXML private Label headerSubtitle;
    @FXML private Label headerName;
    @FXML private Label headerRole;
    @FXML private Label sidebarName;
    @FXML private Label sidebarRole;
    @FXML private Label clockLabel;
    @FXML private Text headerAvatarText;
    @FXML private Text avatarText;
    @FXML private StackPane contentArea;

    private final Map<String, String> screens = new HashMap<>();
    private final Map<String, Button> navButtons = new HashMap<>();
    private User user;
    private String currentScreen;

    @FXML
    private void initialize() {
        screens.put("dashboard", "/fxml/admin-dashboard.fxml");
        screens.put("search", "/fxml/search.fxml");
        screens.put("flights", "/fxml/flights.fxml");
        screens.put("aircraft", "/fxml/aircraft.fxml");
        screens.put("bookings", "/fxml/bookings.fxml");
        screens.put("passengers", "/fxml/passengers.fxml");
        screens.put("tickets", "/fxml/tickets.fxml");
        screens.put("reports", "/fxml/reports.fxml");
        screens.put("profile", "/fxml/profile.fxml");

        navButtons.put("dashboard", navDashboard);
        navButtons.put("search", navSearch);
        navButtons.put("flights", navFlights);
        navButtons.put("aircraft", navAircraft);
        navButtons.put("bookings", userRoleKey());
        navButtons.put("passengers", navPassengers);
        navButtons.put("tickets", navTickets);
        navButtons.put("reports", navReports);
        navButtons.put("profile", navProfile);

        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            clockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM  HH:mm")));
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private Button userRoleKey() {
        return navBookings;
    }

    public void setUser(User user) {
        this.user = user;
        boolean admin = user.getRole() == UserRole.ADMIN;
        adminNav.setVisible(admin);
        adminNav.setManaged(admin);
        passengerNav.setVisible(!admin);
        passengerNav.setManaged(!admin);
        navButtons.put("bookings", admin ? navBookingsAdmin : navBookings);

        String initials = user.getInitials();
        headerAvatarText.setText(initials);
        avatarText.setText(initials);
        headerName.setText(user.getFullName());
        sidebarName.setText(user.getFullName());
        headerRole.setText(user.getRole().getDisplayName());
        sidebarRole.setText(admin ? "Operations team" : "Frequent flyer");
        headerSubtitle.setText(admin ? "Airline operations at a glance"
                : "Your next journey starts here");
    }

    public void navigate(String screen) {
        String fxml = screens.get(screen);
        if (fxml == null) {
            throw new IllegalArgumentException("Unknown screen: " + screen);
        }
        if ("dashboard".equals(screen) && user != null && user.getRole() == UserRole.PASSENGER) {
            fxml = "/fxml/passenger-dashboard.fxml";
        }
        currentScreen = screen;
        contentArea.getChildren().setAll(SceneManager.load(fxml));
        navButtons.values().forEach(button -> {
            if (button != null) {
                button.getStyleClass().remove("nav-item-active");
            }
        });
        Button active = navButtons.get(screen);
        if (active != null) {
            active.getStyleClass().add("nav-item-active");
        }
        headerTitle.setText(titleFor(screen));
    }

    private String titleFor(String screen) {
        return switch (screen) {
            case "dashboard" -> "Dashboard";
            case "search" -> "Search flights";
            case "flights" -> "Flight management";
            case "aircraft" -> "Aircraft fleet";
            case "bookings" -> user.getRole() == UserRole.ADMIN ? "All bookings" : "My bookings";
            case "passengers" -> "Passengers";
            case "tickets" -> "My tickets";
            case "reports" -> "Reports & analytics";
            case "profile" -> "Profile & security";
            default -> "AeroReserve";
        };
    }

    public String getCurrentScreen() {
        return currentScreen;
    }

    @FXML private void handleDashboard(ActionEvent e) { navigate("dashboard"); }
    @FXML private void handleFlights(ActionEvent e) { navigate("flights"); }
    @FXML private void handleAircraft(ActionEvent e) { navigate("aircraft"); }
    @FXML private void handleBookings(ActionEvent e) { navigate("bookings"); }
    @FXML private void handlePassengers(ActionEvent e) { navigate("passengers"); }
    @FXML private void handleReports(ActionEvent e) { navigate("reports"); }
    @FXML private void handleSearch(ActionEvent e) { navigate("search"); }
    @FXML private void handleTickets(ActionEvent e) { navigate("tickets"); }
    @FXML private void handleProfile(ActionEvent e) { navigate("profile"); }

    @FXML
    private void handleLogout(ActionEvent event) {
        ServiceContainer.get().auth().logout();
        ServiceContainer.get().flushAll();
        SceneManager.showLogin();
    }
}
