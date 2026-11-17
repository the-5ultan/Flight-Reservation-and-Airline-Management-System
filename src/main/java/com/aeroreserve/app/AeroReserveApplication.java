package com.aeroreserve.app;

import com.aeroreserve.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point for the AeroReserve desktop application.
 */
public class AeroReserveApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.initialize(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
