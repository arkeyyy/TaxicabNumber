package org.example.taxicab_number;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class TaxicabApplication extends Application {
    private TaxicabController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(TaxicabApplication.class.getResource("taxicab-view.fxml"));
        Scene scene = new Scene(loader.load(), 760, 640);
        controller = loader.getController();
        stage.setTitle("Taxicab Number");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.close();
        }
    }
}
