package com.aeroreserve.controller;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.model.Admin;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.model.User;
import com.aeroreserve.ui.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;

/** Profile editing and password change for both roles. */
public class ProfileController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField nationalityField;
    @FXML private TextField passportField;
    @FXML private TextField roleField;
    @FXML private Label profileMessage;
    @FXML private Label passwordMessage;
    @FXML private PasswordField currentPassword;
    @FXML private PasswordField newPassword;
    @FXML private PasswordField confirmPassword;

    private User user;

    @FXML
    private void initialize() {
        user = ServiceContainer.get().auth().getCurrentUser();
        nameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
        roleField.setText(user.getRole().getDisplayName());
        if (user instanceof Passenger passenger) {
            nationalityField.setText(passenger.getNationality());
            passportField.setText(passenger.getPassportNumber());
        } else if (user instanceof Admin) {
            nationalityField.setDisable(true);
            passportField.setDisable(true);
        }
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        try {
            ServiceContainer.get().auth().updateProfile(nameField.getText(), phoneField.getText(),
                    passportField.getText(), nationalityField.getText());
            ServiceContainer.get().flushAll();
            message(profileMessage, "Profile updated.", true);
            SceneManager.showToast("Profile updated.", true);
        } catch (Exception e) {
            message(profileMessage, e.getMessage(), false);
        }
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        if (!newPassword.getText().equals(confirmPassword.getText())) {
            message(passwordMessage, "New passwords do not match.", false);
            return;
        }
        try {
            ServiceContainer.get().auth().changePassword(currentPassword.getText(), newPassword.getText());
            ServiceContainer.get().flushAll();
            currentPassword.clear();
            newPassword.clear();
            confirmPassword.clear();
            message(passwordMessage, "Password changed successfully.", true);
        } catch (Exception e) {
            message(passwordMessage, e.getMessage(), false);
        }
    }

    private void message(Label label, String text, boolean success) {
        label.setText(text);
        label.getStyleClass().remove("success");
        if (success) {
            label.getStyleClass().add("success");
        }
    }
}
