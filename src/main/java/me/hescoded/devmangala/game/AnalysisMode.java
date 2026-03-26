package me.hescoded.devmangala.game;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.util.Duration;
import me.hescoded.devmangala.ui.BoardView;
import me.hescoded.devmangala.variables.GameResult;
import me.hescoded.devmangala.variables.PlayerSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisMode {
    private final Player p1, p2;
    private int[] board;
    private Player currentPlayer;
    private BoardView view;
    private MoveHandler moveHandler;
    private boolean isAnimationPlaying = false;
    private boolean isEngineThinking = false;
    private Timeline thinkingTimeline;
    private NativeEngine nativeEngine;

    public AnalysisMode(Player p1, Player p2, PlayerSide firstPlayer, BoardView view) {
        this.p1 = p1;
        this.p2 = p2;
        this.view = view;
        this.board = new int[] {4,4,4,4,4,4,0,4,4,4,4,4,4,0};
        this.nativeEngine = new NativeEngine();

        currentPlayer = (firstPlayer == PlayerSide.BOTTOM) ? p1 : p2;
        moveHandler = new MoveHandler();

        view.buttonMap.forEach((id, pitButton) -> {
            pitButton.getButton().setText(String.valueOf(board[id]));
        });
        view.enablePlayerButtons(currentPlayer.getSide(), getZeroButtons());
        startAnalysisForCurrentTurn();
    }

    public void onPitClicked(int pitId) {
        if (isAnimationPlaying) return;
        if (currentPlayer.getSide() == PlayerSide.BOTTOM && pitId > 5) return;
        if (currentPlayer.getSide() == PlayerSide.TOP && pitId < 7) return;
        if (board[pitId] == 0) return;

        view.enablePlayerButtons(null, null);
        MoveResult move = moveHandler.move(board, pitId, currentPlayer.getSide());
        executeMoveSequence(move);
    }

    private void executeMoveSequence(MoveResult move) {
        isAnimationPlaying = true;
        view.playMoveAnimation(move.getPath(), () -> {
            view.buttonMap.forEach((id, pitButton) -> {
                pitButton.getButton().setText(String.valueOf(board[id]));
            });
            isAnimationPlaying = false;
            handleTurnEnd(move);
        });
    }

    private void handleTurnEnd(MoveResult move) {
        if (checkGameState(board) != GameResult.CONTINUES) {
            announceWinner();
            return;
        }

        if (move.isGivesExtraTurn()) {
            if (currentPlayer == p2) {
                view.bottomLabel.setText("Top player's turn.");
            } else {
                view.bottomLabel.setText("Bottom player's turn.");
            }
        } else {
            currentPlayer = (currentPlayer == p1) ? p2 : p1;
            if (currentPlayer == p2) {
                view.bottomLabel.setText("Top player's turn.");
            } else {
                view.bottomLabel.setText("Bottom player's turn.");
            }
        }
        view.enablePlayerButtons(currentPlayer.getSide(), getZeroButtons());
        startAnalysisForCurrentTurn();
    }

    private List<Integer> getZeroButtons() {
        List<Integer> zeroButtons = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (board[i] == 0) zeroButtons.add(i);
            if (board[i+7] == 0) zeroButtons.add(i+7);
        }
        return zeroButtons;
    }

    public void announceWinner() {
        isAnimationPlaying = false;
        view.enablePlayerButtons(null, null);
        collectRemainingStones(board);
        view.buttonMap.forEach((id, pitButton) -> {
            pitButton.getButton().setText(String.valueOf(board[id]));
        });

        String message = switch (checkGameState(board)) {
            case BOTTOM_WON -> "Bottom WON! Score: " + board[6] + " - " + board[13];
            case TOP_WON -> "Top WON! Score: " + board[6] + " - " + board[13];
            case DRAW -> "DRAW! Score: " + board[6] + " - " + board[13];
            default -> "";
        };
        view.bottomLabel.setText(message);
    }

    private GameResult checkGameState(int[] currentBoard) {
        boolean isBottomEmpty = true;
        boolean isTopEmpty = true;

        for (int i = 0; i < 6; i++) {
            if (currentBoard[i] > 0) { isBottomEmpty = false; break; }
        }
        for (int i = 7; i < 13; i++) {
            if (currentBoard[i] > 0) { isTopEmpty = false; break; }
        }

        if (isBottomEmpty || isTopEmpty) {
            int bottomScore = currentBoard[6];
            int topScore = currentBoard[13];

            if (isBottomEmpty) {
                for (int i = 7; i < 13; i++) topScore += currentBoard[i];
            } else {
                for (int i = 0; i < 6; i++) bottomScore += currentBoard[i];
            }

            if (bottomScore > topScore) return GameResult.BOTTOM_WON;
            if (topScore > bottomScore) return GameResult.TOP_WON;
            return GameResult.DRAW;
        }
        return GameResult.CONTINUES;
    }

    private void collectRemainingStones(int[] currentBoard) {
        int bottomStones = 0, topStones = 0;
        for (int i = 0; i < 6; i++) { bottomStones += currentBoard[i]; currentBoard[i] = 0; }
        for (int i = 7; i < 13; i++) { topStones += currentBoard[i]; currentBoard[i] = 0; }
        if (bottomStones == 0) currentBoard[6] += topStones;
        if (topStones == 0) currentBoard[13] += bottomStones;
    }

    private void startAnalysisForCurrentTurn() {
        if (!view.isEngineEnabled()) {
            view.bottomLabel.setText((currentPlayer.getSide() == PlayerSide.BOTTOM ? "Bottom" : "Top") + " player's turn. Analysis is off." );
            view.enablePlayerButtons(currentPlayer.getSide(), getZeroButtons());
            // isEngineThinking = false;
            return;
        }

        startThinkingAnimation();
        view.enablePlayerButtons(null, null);

        int depth = view.getEngineDepth();

        Task<Map<Integer, Integer>> analysisTask = new Task<>() {
            @Override
            protected Map<Integer, Integer> call() throws Exception {
                Map<Integer, Integer> evaluations = new HashMap<>();

                int startIdx = (currentPlayer.getSide() == PlayerSide.BOTTOM) ? 0 : 7;
                int endIdx = (currentPlayer.getSide() == PlayerSide.BOTTOM) ? 5 : 12;

                for (int i = startIdx; i <= endIdx; i++) {
                    if (board[i] > 0) {
                        int[] tempBoard = board.clone();
                        MoveResult result = moveHandler.move(tempBoard, i, currentPlayer.getSide());
                        boolean isCurrentTop = currentPlayer.getSide() == PlayerSide.TOP;
                        boolean isNextTop = isCurrentTop;
                        if (!result.isGivesExtraTurn()) isNextTop = !isCurrentTop;
                        int score = (nativeEngine.findBestMove(tempBoard, depth, isNextTop)[1]);
                        evaluations.put(i, score);
                    }
                }
                return evaluations;
            }
        };

        analysisTask.setOnSucceeded(event -> {
            Map<Integer, Integer> results = analysisTask.getValue();

            results.forEach((pitId, score) -> {
                int stones = board[pitId];
                String buttonText = stones + "\n(" + (score > 0 ? "+" : "") + score + ")";
                view.buttonMap.get(pitId).getButton().setText(buttonText);
            });

            view.enablePlayerButtons(currentPlayer.getSide(), getZeroButtons());
            view.bottomLabel.setText(currentPlayer.getSide() == PlayerSide.BOTTOM ? "Bottom player's turn." : "Top player's turn.");
            stopThinkingAnimation();
        });

        new Thread(analysisTask).start();
    }

    private void startThinkingAnimation() {
        int depth = view.getEngineDepth();
        String analyzingText = "Analyzing moves (Depth: " +  depth + ")";
        thinkingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> view.bottomLabel.setText(analyzingText + ".")),
                new KeyFrame(Duration.seconds(1.0), e -> view.bottomLabel.setText(analyzingText + "..")),
                new KeyFrame(Duration.seconds(1.5), e -> view.bottomLabel.setText(analyzingText + "..."))
        );
        thinkingTimeline.setCycleCount(Animation.INDEFINITE);
        thinkingTimeline.play();
    }

    private void stopThinkingAnimation() {
        if (thinkingTimeline != null) thinkingTimeline.stop();
    }
}
