package com.aeroreserve.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controls the login and passenger registration screen.
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox showPasswordBox;
    @FXML private Label messageLabel;
    @FXML private VBox registerBox;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField nationalityField;
    @FXML private TextField passportField;
    @FXML private PasswordField confirmField;

    @FXML
    private void initialize() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    @FXML
    private void handleShowPassword(ActionEvent event) {
        passwordField.setOpacity(showPasswordBox.isSelected() ? 0 : 1);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        showMessage("Authentication services are being initialised.", false);
    }

    @FXML
    private void handleRegisterToggle(ActionEvent event) {
        boolean opening = !registerBox.isVisible();
        registerBox.setVisible(opening);
        registerBox.setManaged(opening);
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        showMessage("Registration services are being initialised.", false);
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        showMessage("Please contact your administrator to reset a password.", false);
    }

    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.getStyleClass().removeAll("success");
        if (success) {
            messageLabel.getStyleClass().add("success");
        }
    }
}
