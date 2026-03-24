package me.hescoded.devmangala.ui;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import me.hescoded.devmangala.game.GameControllerPvC;
import me.hescoded.devmangala.game.Player;
import me.hescoded.devmangala.variables.PlayerSide;
import me.hescoded.devmangala.variables.PlayerType;

import java.util.HashMap;
import java.util.List;

public class BoardView {
    public HashMap<Integer, PitButton> buttonMap;
    public Label bottomLabel;
    public BorderPane addBorderPane() {
        BorderPane borderPane = new BorderPane();
        buttonMap = new HashMap<>();

        HBox hbox = new HBox();
        Button btnGame = new Button("Game");
        Button btnAbout = new Button("About");
        Button btnRules = new Button("Rules");
        hbox.getChildren().addAll(btnGame, btnAbout, btnRules);
        borderPane.setTop(hbox);

        btnGame.setOnAction(actionEvent -> {
            Stage stageGameMenu = new Stage();
            stageGameMenu.setTitle("New Game");
            VBox rootGameMenu = new VBox();
            rootGameMenu.setAlignment(Pos.CENTER);
            Scene sceneGameMenu = new Scene(rootGameMenu, 350, 300);

            VBox panelGameTypeChoice = new VBox();
            panelGameTypeChoice.setAlignment(Pos.CENTER);
            VBox.setMargin(panelGameTypeChoice, new Insets(0, 0, 30, 0));
            ToggleGroup tgGameTypeChoice = new ToggleGroup();
            RadioButton rbPlayerVsComputer = new RadioButton("Player vs Computer");
            RadioButton rbComputerVsComputer = new RadioButton("Computer vs Computer");
            RadioButton rbAnalysis = new RadioButton("Analysis");
            rbPlayerVsComputer.setSelected(true);
            rbPlayerVsComputer.setToggleGroup(tgGameTypeChoice);
            rbComputerVsComputer.setToggleGroup(tgGameTypeChoice);
            rbAnalysis.setToggleGroup(tgGameTypeChoice);
            panelGameTypeChoice.getChildren().add(rbPlayerVsComputer);
            panelGameTypeChoice.getChildren().add(rbComputerVsComputer);
            panelGameTypeChoice.getChildren().add(rbAnalysis);

            VBox depthPane = new VBox();
            VBox.setMargin(depthPane, new Insets(0, 0, 30, 0));
            Integer[] depthOptions = {1, 3, 5, 8, 12, 14, 16, 20, 100};

            GridPane gpDepthPvC = new GridPane(2, 1);
            gpDepthPvC.setAlignment(Pos.CENTER);
            gpDepthPvC.add(new Text("Computer Depth: "), 0, 0);
            ComboBox<Integer> cbDepthPvC = new ComboBox<>(FXCollections.observableArrayList(depthOptions));
            cbDepthPvC.getSelectionModel().selectFirst();
            gpDepthPvC.add(cbDepthPvC, 1, 0);
            depthPane.getChildren().add(gpDepthPvC);

            GridPane gpDepthCvC = new GridPane(2, 2);
            gpDepthCvC.setAlignment(Pos.CENTER);
            gpDepthCvC.add(new Text("Top Computer Depth: "), 0, 0);
            ComboBox<Integer> cbDepthCvCTop = new ComboBox<>(FXCollections.observableArrayList(depthOptions));
            cbDepthCvCTop.getSelectionModel().selectFirst();
            gpDepthCvC.add(cbDepthCvCTop, 1, 0);
            gpDepthCvC.add(new Text("Bottom Computer Depth: "), 0, 1);
            ComboBox<Integer> cbDepthCvCBottom = new ComboBox<>(FXCollections.observableArrayList(depthOptions));
            cbDepthCvCBottom.getSelectionModel().selectFirst();
            gpDepthCvC.add(cbDepthCvCBottom, 1, 1);

            VBox starterPane = new VBox();
            VBox.setMargin(starterPane, new Insets(0, 0, 30, 0));

            GridPane gpStarter = new GridPane(2, 1);
            gpStarter.setAlignment(Pos.CENTER);
            gpStarter.add(new Text("Who goes first: "), 0, 0);
            String[] starterOptionsPvC = {"PLAYER", "COMPUTER"};
            String[] starterOptionsCvC = {"BOTTOM", "TOP"};
            ComboBox<String> cbStarter = new ComboBox<>(FXCollections.observableArrayList(starterOptionsPvC));
            cbStarter.getSelectionModel().selectFirst();
            gpStarter.add(cbStarter, 1, 0);
            starterPane.getChildren().add(gpStarter);

            Button btnStartGame = new Button("Start Game");
            btnStartGame.setAlignment(Pos.CENTER);
            btnStartGame.setOnAction(actionEvent1 -> {
                if (rbPlayerVsComputer.isSelected()) {
                    stageGameMenu.close();
                    PlayerSide turnPlayer = (cbStarter.getValue().equals("PLAYER")) ? PlayerSide.BOTTOM : PlayerSide.TOP;
                    int computerDepth = cbDepthPvC.getValue();
                    GameControllerPvC game = new GameControllerPvC(new Player(PlayerSide.BOTTOM, PlayerType.HUMAN, 1),
                            new Player(PlayerSide.TOP, PlayerType.COMPUTER, computerDepth),
                            turnPlayer, this);
                    buttonMap.forEach((id, pitButton) -> {
                        pitButton.getButton().setOnAction(e -> {
                            game.onPitClicked(id);
                        });
                    });
                }
                if (rbComputerVsComputer.isSelected()) {
                    // TODO
                }
                if (rbAnalysis.isSelected()) {
                    // TODO
                }
            });

            rbPlayerVsComputer.setOnAction(actionEvent1 -> {
                depthPane.getChildren().clear();
                depthPane.getChildren().add(gpDepthPvC);
                VBox.setMargin(depthPane, new Insets(0, 0, 30, 0));
                cbStarter.setItems(FXCollections.observableArrayList(starterOptionsPvC));
                cbStarter.getSelectionModel().selectFirst();
            });
            rbComputerVsComputer.setOnAction(actionEvent1 -> {
                depthPane.getChildren().clear();
                depthPane.getChildren().add(gpDepthCvC);
                VBox.setMargin(depthPane, new Insets(0, 0, 30, 0));
                cbStarter.setItems(FXCollections.observableArrayList(starterOptionsCvC));
                cbStarter.getSelectionModel().selectFirst();
            });
            rbAnalysis.setOnAction(actionEvent1 -> {
                depthPane.getChildren().clear();
                VBox.setMargin(depthPane, new Insets(0, 0, 0, 0));
                cbStarter.setItems(FXCollections.observableArrayList(starterOptionsCvC));
                cbStarter.getSelectionModel().selectFirst();
            });

            rootGameMenu.getChildren().add(panelGameTypeChoice);
            rootGameMenu.getChildren().add(depthPane);
            rootGameMenu.getChildren().add(starterPane);
            rootGameMenu.getChildren().add(btnStartGame);

            stageGameMenu.setScene(sceneGameMenu);
            stageGameMenu.show();
        });

        btnAbout.setOnAction(actionEvent -> {/*TODO*/});
        btnRules.setOnAction(actionEvent -> {/*TODO*/});

        PitButton storeLeft = new PitButton(13, true);
        borderPane.setLeft(storeLeft.getButton());
        buttonMap.put(13, storeLeft);
        PitButton storeRight = new PitButton(6, true);
        borderPane.setRight(storeRight.getButton());
        buttonMap.put(6, storeRight);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(0);
        gridPane.setVgap(0);
        for (int i = 0; i < 12; i++) {
            PitButton houseButton;
            int houseId;
            if (i < 6) {
                houseId = i;
                houseButton = new PitButton(houseId, false);
                gridPane.add(houseButton.getButton(), i, 1);
            } else {
                houseId = i + 1;
                houseButton = new PitButton(houseId, false);
                gridPane.add(houseButton.getButton(), 11 - i, 0);
            }
            buttonMap.put(houseId, houseButton);
        }
        borderPane.setCenter(gridPane);

        bottomLabel = new Label("HesCoded tarafından yapılmıştır.");
        bottomLabel.setPadding(new Insets(0, 0, 0, 20));
        borderPane.setBottom(bottomLabel);

        return borderPane;
    }

    public void enablePlayerButtons(PlayerSide side) {
        buttonMap.forEach((id, pitButton) -> {
            pitButton.getButton().setDisable(true);
            pitButton.getButton().setStyle("-fx-background-color: #5d6d7e; -fx-opacity: 1.0; -fx-border-color: #000000; -fx-text-fill: #000000");
            if (side == PlayerSide.BOTTOM && id < 6) {
                pitButton.getButton().setStyle("-fx-background-color: #ffffff; -fx-opacity: 1.0; -fx-border-color: #000000; -fx-text-fill: #000000");
                pitButton.getButton().setDisable(false);
            }
            if (side == PlayerSide.TOP && id > 6 && id != 13) {
                pitButton.getButton().setStyle("-fx-background-color: #ffffff; -fx-opacity: 1.0; -fx-border-color: #000000; -fx-text-fill: #000000");
                pitButton.getButton().setDisable(false);
            }
        });
    }

    public void playMoveAnimation(List<Integer> path, Runnable onAnimationFinished) {
        Timeline timeline = new Timeline();
        int interval = 500;

        for (int i = 0; i < path.size(); i++) {
            int currentIndex = i;
            int pitId = path.get(i);
            Button targetButton = buttonMap.get(pitId).getButton();

            KeyFrame keyFrame = new KeyFrame(Duration.millis((i + 1) * interval), event -> {
                String oldStyle = targetButton.getStyle();
                targetButton.setStyle("-fx-background-color: #f1c40f; -fx-border-color: black; -fx-text-fill: black;");

                int currentStones = Integer.parseInt(targetButton.getText());

                if (currentIndex == 0 && currentStones == 1) targetButton.setText(String.valueOf(0));
                else if (currentIndex == 0 && currentStones > 1) targetButton.setText(String.valueOf(1));
                else targetButton.setText(String.valueOf(currentStones + 1));

                PauseTransition flashOut = new PauseTransition(Duration.millis(250));
                flashOut.setOnFinished(e -> targetButton.setStyle(oldStyle));
                flashOut.play();
            });

            timeline.getKeyFrames().add(keyFrame);
        }

        timeline.setOnFinished(e -> {
            if (onAnimationFinished != null)
                onAnimationFinished.run();
        });

        timeline.play();
    }
}
