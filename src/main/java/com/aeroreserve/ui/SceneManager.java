package com.aeroreserve.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Central navigator for every screen in the application.
 * Keeps FXML loading, scene switching and window styling in one place.
 */
public final class SceneManager {

    private static Stage stage;
    private static Scene scene;

    private SceneManager() {
    }

    public static void initialize(Stage primaryStage) {
        stage = Objects.requireNonNull(primaryStage, "primaryStage");
        stage.setTitle("AeroReserve - Flight Reservation & Airline Management");
        scene = new Scene(load("/fxml/login.fxml"), 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                SceneManager.class.getResource("/css/theme.css"), "theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.centerOnScreen();
        stage.show();
    }

    public static Parent load(String fxmlPath) {
        try {
            return new FXMLLoader(Objects.requireNonNull(
                    SceneManager.class.getResource(fxmlPath), "FXML resource: " + fxmlPath)).load();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load FXML resource: " + fxmlPath, e);
        }
    }

    public static Stage getStage() {
        return stage;
    }

    public static Scene getScene() {
        return scene;
    }
}
