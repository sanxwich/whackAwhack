package com.whackawhack.whackawhack;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class WhackApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(WhackApplication.class.getResource("game.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 861, 610);
        stage.setTitle("Whack A Whack");
        stage.setScene(scene);
        stage.show();
    }
}