package me.hescoded.devmangala;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.hescoded.devmangala.ui.BoardView;

import java.io.IOException;

public class DevMangala extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        BoardView boardView = new BoardView();
        BorderPane root = boardView.addBorderPane();
        Scene scene = new Scene(root, 800, 300);

        stage.setTitle("DevMangala");
        stage.setScene(scene);
        stage.setOnCloseRequest(windowEvent -> System.exit(0));
        stage.show();
    }
}
