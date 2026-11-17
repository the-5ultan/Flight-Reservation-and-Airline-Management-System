package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.exception.AuthenticationException;
import com.aeroreserve.exception.DuplicateUserException;
import com.aeroreserve.model.User;
import com.aeroreserve.ui.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Controls the login and passenger registration screen. */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
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
        clearMessage();
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
    }

    @FXML
    private void handleShowPassword(ActionEvent event) {
        boolean visible = showPasswordBox.isSelected();
        visiblePasswordField.setVisible(visible);
        visiblePasswordField.setManaged(visible);
        passwordField.setVisible(!visible);
        passwordField.setManaged(!visible);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        clearMessage();
        String password = passwordField.getText();
        if (emailField.getText().isBlank() || password.isBlank()) {
            showMessage("Enter both your email address and password.", false);
            return;
        }
        try {
            User user = ServiceContainer.get().auth().login(emailField.getText(), password);
            passwordField.clear();
            ServiceContainer.get().flushAll();
            SceneManager.showMainShell(user);
        } catch (AuthenticationException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void handleRegisterToggle(ActionEvent event) {
        boolean opening = !registerBox.isVisible();
        registerBox.setVisible(opening);
        registerBox.setManaged(opening);
        clearMessage();
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        clearMessage();
        String password = passwordField.getText();
        if (!password.equals(confirmField.getText())) {
            showMessage("Passwords do not match.", false);
            return;
        }
        try {
            ServiceContainer.get().auth().registerPassenger(
                    nameField.getText(), emailField.getText(), password,
                    phoneField.getText(), passportField.getText(), nationalityField.getText());
            ServiceContainer.get().flushAll();
            registerBox.setVisible(false);
            registerBox.setManaged(false);
            emailField.setText(emailField.getText());
            showMessage("Account created. You can sign in now.", true);
            confirmField.clear();
        } catch (DuplicateUserException | AuthenticationException | IllegalArgumentException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        showMessage("Ask an administrator to reset your password from the Users panel.", false);
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

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }
}
