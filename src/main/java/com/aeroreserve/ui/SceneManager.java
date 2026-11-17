package com.aeroreserve.ui;

import com.aeroreserve.app.ServiceContainer;
import com.aeroreserve.controller.ShellController;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;

/** Central navigator, toast host and screen loader. */
public final class SceneManager {

    private static Stage stage;
    private static Scene scene;
    private static StackPane rootWrapper;
    private static ShellController shellController;

    private SceneManager() {
    }

    public static void initialize(Stage primaryStage) {
        stage = Objects.requireNonNull(primaryStage, "primaryStage");
        try {
            ServiceContainer.initialize(Paths.get("data"));
        } catch (IOException e) {
            DialogManager.showError("Startup failed", "Application data could not be loaded: " + e.getMessage());
            return;
        }
        rootWrapper = new StackPane();
        scene = new Scene(rootWrapper, 1400, 860);
        scene.getStylesheets().add(Objects.requireNonNull(
                SceneManager.class.getResource("/css/theme.css"), "theme.css").toExternalForm());
        stage.setTitle("AeroReserve - Flight Reservation & Airline Management");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.centerOnScreen();
        stage.setOnCloseRequest(event -> ServiceContainer.get().flushAll());
        showLogin();
        stage.show();
    }

    public static Parent load(String fxmlPath) {
        return load(fxmlPath, controller -> { });
    }

    /** Loads FXML and lets the caller configure the controller before it is displayed. */
    public static Parent load(String fxmlPath, Consumer<Object> configurator) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    SceneManager.class.getResource(fxmlPath), "FXML resource: " + fxmlPath));
            Parent root = loader.load();
            if (loader.getController() != null) {
                configurator.accept(loader.getController());
            }
            return root;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load FXML resource: " + fxmlPath, e);
        }
    }

    public static void showLogin() {
        shellController = null;
        setContent(load("/fxml/login.fxml"));
    }

    public static void showMainShell(com.aeroreserve.model.User user) {
        Parent shell = load("/fxml/main-shell.fxml", controller -> {
            shellController = (ShellController) controller;
            shellController.setUser(user);
        });
        setContent(shell);
        navigate("dashboard");
    }

    public static void navigate(String screen) {
        if (shellController != null) {
            shellController.navigate(screen);
        }
    }

    public static void setContent(Parent content) {
        rootWrapper.getChildren().setAll(content);
    }

    public static Stage getStage() {
        return stage;
    }

    public static Scene getScene() {
        return scene;
    }

    public static void showToast(String message, boolean success) {
        if (rootWrapper == null) {
            return;
        }
        Label toast = new Label(message);
        toast.getStyleClass().addAll("toast", success ? "toast-success" : "toast-error");
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 28, 0));
        toast.setOpacity(0);
        rootWrapper.getChildren().add(toast);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
        fadeIn.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.seconds(2.8));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(320), toast);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> rootWrapper.getChildren().remove(toast));
        javafx.animation.SequentialTransition sequence =
                new javafx.animation.SequentialTransition(fadeIn, hold, fadeOut);
        sequence.play();
    }
}
