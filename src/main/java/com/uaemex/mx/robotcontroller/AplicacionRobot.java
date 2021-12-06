package com.uaemex.mx.robotcontroller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class AplicacionRobot extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AplicacionRobot.class.getResource("VistaRobot.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 700);
        stage.setTitle("Robotica Avanzada 2021b");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}