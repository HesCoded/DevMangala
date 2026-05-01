package me.hescoded.devmangala.ui;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import me.hescoded.devmangala.game.GameController;
import me.hescoded.devmangala.game.Player;
import me.hescoded.devmangala.variables.PlayerSide;
import org.controlsfx.control.ToggleSwitch;

import java.util.HashMap;
import java.util.List;

public class BoardView {
    public HashMap<Integer, PitButton> buttonMap;
    public Label bottomLabel, topSideLabel, bottomSideLabel;
    public ToggleSwitch engineToggle;
    public Slider depthSlider;

    private BorderPane gamePane;
    private int currentPitsPerPlayer = 6;

    public BorderPane addBorderPane(Stage primaryStage) {
        BorderPane borderPane = new BorderPane();
        borderPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        buttonMap = new HashMap<>();

        HBox hbox = new HBox();
        Button btnGame = new Button("Game");
        Button btnAbout = new Button("About");
        Button btnRules = new Button("Rules");
        hbox.getChildren().addAll(btnGame, btnAbout, btnRules);
        borderPane.setTop(hbox);

        gamePane = new BorderPane();
        borderPane.setCenter(gamePane);

        HBox topSidePane = new HBox();
        topSidePane.setAlignment(Pos.BOTTOM_LEFT);
        topSidePane.getStyleClass().add("side-pane");
        topSideLabel = new Label();
        topSideLabel.getStyleClass().add("player-label");
        gamePane.setTop(topSidePane);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controlPane = new HBox(10);
        controlPane.setAlignment(Pos.CENTER_RIGHT);
        controlPane.setPadding(new Insets(5, 5, 5, 5));

        engineToggle = new ToggleSwitch();
        // engineToggle.getStyleClass().add("engine-toggle");
        engineToggle.setSelected(false);
        // engineToggle.textProperty().bind(Bindings.when(engineToggle.selectedProperty()).then("Analysis: On").otherwise("Analysis: Off"));

        depthSlider = new Slider(12, 20, 16);
        depthSlider.setShowTickLabels(true);
        depthSlider.setMajorTickUnit(2);
        depthSlider.setMinorTickCount(0);
        depthSlider.setSnapToTicks(true);
        depthSlider.setMinWidth(200);

        controlPane.getChildren().addAll(engineToggle, depthSlider);
        topSidePane.getChildren().addAll(topSideLabel, spacer, controlPane);

        VBox bottomSidePane = new VBox();
        bottomSidePane.getStyleClass().add("side-pane");
        bottomSideLabel = new Label();
        bottomSideLabel.getStyleClass().add("player-label");
        bottomSidePane.getChildren().add(bottomSideLabel);
        gamePane.setBottom(bottomSidePane);

        buildBoard(6, 4);

        bottomLabel = new Label("Developed by HesCoded.");
        bottomLabel.getStyleClass().add("bottom-label");
        borderPane.setBottom(bottomLabel);

        btnGame.setOnAction(actionEvent -> {
            RadioButton rbPvC = new RadioButton("Player vs Computer");
            RadioButton rbAnalysis = new RadioButton("Analysis");
            rbPvC.getStyleClass().add("newgame-menu-radio");
            rbAnalysis.getStyleClass().add("newgame-menu-radio");

            ToggleGroup gameModeGroup = new ToggleGroup();
            rbPvC.setToggleGroup(gameModeGroup);
            rbAnalysis.setToggleGroup(gameModeGroup);
            rbPvC.setSelected(true);

            VBox radioBox = new VBox(10, rbPvC, rbAnalysis);

            Separator separator = new Separator();

            Label lbDepth = new Label("Computer Depth:");
            lbDepth.setPrefWidth(110.0f);
            lbDepth.getStyleClass().add("newgame-menu-label");
            ComboBox<Integer> comboDepth = new ComboBox<>(FXCollections.observableArrayList(1, 3, 5, 8, 10, 12, 14, 16, 18, 20, 100));
            comboDepth.getSelectionModel().selectFirst();
            comboDepth.setPrefWidth(70.0f);
            comboDepth.getStyleClass().add("newgame-menu-combo");
            HBox depthBox = new HBox(10, lbDepth, comboDepth);
            depthBox.setAlignment(Pos.CENTER_LEFT);

            Label lbPits = new Label("Pits per Player:");
            lbPits.setPrefWidth(110.0f);
            lbPits.getStyleClass().add("newgame-menu-label");
            ComboBox<Integer> comboPits = new ComboBox<>(FXCollections.observableArrayList(4, 5, 6, 7));
            comboPits.getSelectionModel().select(2);
            comboPits.setPrefWidth(70.0f);
            comboPits.getStyleClass().add("newgame-menu-combo");
            HBox pitsBox = new HBox(10, lbPits, comboPits);
            pitsBox.setAlignment(Pos.CENTER_LEFT);

            Label lbStones = new Label("Stones per Pit:");
            lbStones.setPrefWidth(110.0f);
            lbStones.getStyleClass().add("newgame-menu-label");
            ComboBox<Integer> comboStones = new ComboBox<>(FXCollections.observableArrayList(3, 4, 5, 6));
            comboStones.getSelectionModel().select(1);
            comboStones.setPrefWidth(70.0f);
            comboStones.getStyleClass().add("newgame-menu-combo");
            HBox stonesBox = new HBox(10, lbStones, comboStones);
            stonesBox.setAlignment(Pos.CENTER_LEFT);

            Label lbFirst = new Label("Who goes first:");
            lbFirst.setPrefWidth(110.0f);
            lbFirst.getStyleClass().add("newgame-menu-label");
            ComboBox<String> comboFirst = new ComboBox<>();
            comboFirst.setItems(FXCollections.observableArrayList("Player", "Computer"));
            comboFirst.getSelectionModel().selectFirst();
            comboFirst.getStyleClass().add("newgame-menu-combo");
            HBox firstBox = new HBox(10, lbFirst, comboFirst);
            firstBox.setAlignment(Pos.CENTER_LEFT);

            VBox optionsBox = new VBox(10);
            optionsBox.getChildren().addAll(depthBox, pitsBox, stonesBox, firstBox);

            gameModeGroup.selectedToggleProperty().addListener(((observable, oldValue, newValue) -> {
                if (newValue == rbPvC) {
                    comboDepth.setDisable(false);
                    comboFirst.setItems(FXCollections.observableArrayList("Player", "Computer"));
                    comboFirst.getSelectionModel().selectFirst();
                }
                if (newValue == rbAnalysis) {
                    comboDepth.setDisable(true);
                    comboFirst.setItems(FXCollections.observableArrayList("Bottom", "Top"));
                    comboFirst.getSelectionModel().selectFirst();
                }
            }));

            Button btnStart = new Button("Start Game");
            btnStart.getStyleClass().add("newgame-start-button");

            VBox root = new VBox(25);
            root.getStyleClass().add("newgame-root-pane");
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.TOP_CENTER);

            VBox.setMargin(btnStart, new Insets(10, 0, 0, 0));

            root.getChildren().addAll(radioBox, separator, optionsBox, btnStart);
            // root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); // My CSS plan is not ready, so I won't use them for now!

            Scene scene = new Scene(root, 300, 350);

            Stage stageNewGame = new Stage();
            stageNewGame.setTitle("New Game");
            stageNewGame.setScene(scene);
            stageNewGame.setResizable(false);
            stageNewGame.show();

            btnStart.setOnAction(e -> {
                GameController.GameMode selectedMode = rbPvC.isSelected() ? GameController.GameMode.PVC : GameController.GameMode.ANALYSIS;
                int depth = (comboDepth.getValue() != null) ? comboDepth.getValue() : 1;
                int pitsPerPlayer = (comboPits.getValue() != null) ? comboPits.getValue() : 6;
                int stonesPerPit = (comboStones.getValue() != null) ? comboStones.getValue() : 4;
                PlayerSide turnPlayer = (comboFirst.getValue().equals("Player") || comboFirst.getValue().equals("Bottom")) ? PlayerSide.BOTTOM : PlayerSide.TOP;

                buildBoard(pitsPerPlayer, stonesPerPit);

                double newWidth = (pitsPerPlayer * 100) + 200;
                primaryStage.setWidth(newWidth);

                if (selectedMode == GameController.GameMode.PVC) {
                    topSideLabel.setText("Computer (Depth: " + depth + ")");
                    bottomSideLabel.setText("You");
                } else {
                    topSideLabel.setText("Top Player");
                    bottomSideLabel.setText("Bottom Player");
                }

                GameController game = new GameController(new Player(PlayerSide.BOTTOM, 1),
                        new Player(PlayerSide.TOP, depth), turnPlayer, this, selectedMode, pitsPerPlayer, stonesPerPit);

                buttonMap.forEach((id, pitButton) -> {
                    pitButton.getButton().setOnAction(e2 -> game.onPitClicked(id));
                });

                stageNewGame.close();
            });
        });

        btnAbout.setOnAction(actionEvent -> {/*TODO*/});
        btnRules.setOnAction(actionEvent -> {/*TODO*/});

        return borderPane;
    }

    private void buildBoard(int pitsPerPlayer, int stonesPerPit) {
        this.currentPitsPerPlayer = pitsPerPlayer;
        buttonMap.clear();

        gamePane.setLeft(null);
        gamePane.setCenter(null);
        gamePane.setRight(null);

        int rightStoreId = pitsPerPlayer;
        int leftStoreId = pitsPerPlayer * 2 + 1;

        PitButton storeLeft = new PitButton(leftStoreId, true);
        storeLeft.getButton().getStyleClass().add("store-button");
        storeLeft.getButton().setText("0");
        gamePane.setLeft(storeLeft.getButton());
        buttonMap.put(leftStoreId, storeLeft);

        PitButton storeRight = new PitButton(rightStoreId, true);
        storeRight.getButton().getStyleClass().add("store-button");
        storeRight.getButton().setText("0");
        gamePane.setRight(storeRight.getButton());
        buttonMap.put(rightStoreId, storeRight);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(0);
        gridPane.setVgap(0);
        gridPane.setAlignment(Pos.CENTER);

        for (int i = 0; i < pitsPerPlayer * 2; i++) {
            PitButton houseButton;
            int houseId;
            if (i < pitsPerPlayer) {
                houseId = i;
                houseButton = new PitButton(houseId, false);
                gridPane.add(houseButton.getButton(), i, 1);
            } else {
                houseId = i + 1;
                houseButton = new PitButton(houseId, false);
                gridPane.add(houseButton.getButton(), (pitsPerPlayer * 2) - 1 - i, 0);
            }
            houseButton.getButton().setText(String.valueOf(stonesPerPit));
            houseButton.getButton().getStyleClass().add("pit-button");
            buttonMap.put(houseId, houseButton);
        }
        gamePane.setCenter(gridPane);
    }

    public boolean isEngineEnabled() {
        return engineToggle.isSelected();
    }

    public int getEngineDepth() {
        return (int) depthSlider.getValue();
    }

    public void enablePlayerButtons(PlayerSide side, List<Integer> zeroButtons) {
        buttonMap.forEach((id, pitButton) -> {
            Button btn = pitButton.getButton();
            btn.setDisable(true);
            btn.getStyleClass().remove("animated");

            if (zeroButtons != null) {
                if (!zeroButtons.contains(id)) {
                    boolean isBottomTurn = (side == PlayerSide.BOTTOM && id < currentPitsPerPlayer);
                    boolean isTopTurn = (side == PlayerSide.TOP && id > currentPitsPerPlayer && id != currentPitsPerPlayer * 2 + 1);

                    if (isBottomTurn || isTopTurn) {
                        btn.setDisable(false);
                        if (!btn.getStyleClass().contains("animated")) {
                            btn.getStyleClass().add("animated");
                        }
                    }
                }
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

                int currentStones = Integer.parseInt(targetButton.getText().split("\n")[0]);

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
