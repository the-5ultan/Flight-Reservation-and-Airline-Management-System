package com.aeroreserve.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Reusable modal and alert helpers. */
public final class DialogManager {

    private DialogManager() {
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                DialogManager.class.getResource("/css/theme.css")).toExternalForm());
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                DialogManager.class.getResource("/css/theme.css")).toExternalForm());
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                DialogManager.class.getResource("/css/theme.css")).toExternalForm());
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static Stage openModal(String fxmlPath, String title, double width, double height,
                                  Consumer<Object> configurator) {
        Stage modal = new Stage();
        modal.initOwner(SceneManager.getStage());
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initStyle(StageStyle.DECORATED);
        Parent root = SceneManager.load(fxmlPath, configurator);
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(Objects.requireNonNull(
                DialogManager.class.getResource("/css/theme.css")).toExternalForm());
        modal.setScene(scene);
        modal.setTitle(title);
        modal.setResizable(false);
        modal.centerOnScreen();
        modal.show();
        return modal;
    }
}
