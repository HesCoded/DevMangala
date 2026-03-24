package me.hescoded.devmangala.ui;

import javafx.scene.control.Button;

public class PitButton {
    private final Button button;
    private final int pitId;

    public PitButton(int pitId, boolean isStore) {
        this.pitId = pitId;
        button = new Button();
        if (isStore) {
            button.setPrefSize(100, 200);
        } else {
            button.setPrefSize(100, 100);
        }
        button.setStyle("-fx-background-color: #5d6d7e; -fx-opacity: 1.0; -fx-border-color: #000000; -fx-text-fill: #000000");
        button.setDisable(true);
    }

    public Button getButton() {
        return button;
    }

    public int getPitId() {
        return pitId;
    }
}
