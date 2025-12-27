package com.mcserver.launcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TestApp extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Hello JavaFX!");
        VBox root = new VBox(label);
        Scene scene = new Scene(root, 300, 200);
        
        primaryStage.setTitle("Test Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}