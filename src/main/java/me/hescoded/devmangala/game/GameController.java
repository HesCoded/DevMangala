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

public class GameController {
    public enum GameMode {
        PVC, ANALYSIS
    }

    private final GameMode mode;
    private final Player p1, p2;
    private int[] board;
    private final int PIT_PER_PLAYER, STONES_PER_PIT;
    private Player currentPlayer;
    private BoardView view;
    private MoveHandler moveHandler;
    private boolean isAnimationPlaying = false;
    private Timeline thinkingTimeline;
    private NativeEngine nativeEngine;
    private Thread analysisThread;

    public GameController(Player p1, Player p2, PlayerSide firstPlayer, BoardView view, GameMode mode, int pitPerPlayer, int stonesPerPit) {
        this.p1 = p1;
        this.p2 = p2;
        this.view = view;
        this.mode = mode;
        PIT_PER_PLAYER = pitPerPlayer;
        STONES_PER_PIT = stonesPerPit;
        this.board = new int[(PIT_PER_PLAYER + 1) * 2];
        for (int i = 0; i < board.length; i++) {
            if (i == PIT_PER_PLAYER || i == (PIT_PER_PLAYER * 2) + 1) this.board[i] = 0;
            else this.board[i] = STONES_PER_PIT;
        }
        this.nativeEngine = new NativeEngine();

        currentPlayer = (firstPlayer == PlayerSide.BOTTOM) ? p1 : p2;
        moveHandler = new MoveHandler();

        view.buttonMap.forEach((id, pitButton) -> {
            pitButton.getButton().setText(String.valueOf(board[id]));
        });

        if (mode == GameMode.ANALYSIS) {
            view.enablePlayerButtons(currentPlayer.getSide(), getZeroButtons());

            view.engineToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                nativeEngine.startSearch();
                restartAnalysis();
            });
            view.depthSlider.valueChangingProperty().addListener((observable, oldValue, newValue) -> {
                if (!view.isEngineEnabled()) return;
                if (!newValue) {
                    nativeEngine.startSearch();
                    restartAnalysis();
                }
            });
            startAnalysisForCurrentTurn();

        } else if (mode == GameMode.PVC) {
            if (firstPlayer == PlayerSide.BOTTOM) {
                view.enablePlayerButtons(currentPlayer.getSide(), getZeroButtons());
            } else {
                makeComputerMove();
            }
        }
    }

    public void onPitClicked(int pitId) {
        if (isAnimationPlaying || board[pitId] == 0) return;

        if (mode == GameMode.PVC) {
            if (currentPlayer.getSide() != PlayerSide.BOTTOM) return;
        } else if (mode == GameMode.ANALYSIS) {
            if (currentPlayer.getSide() == PlayerSide.BOTTOM && pitId >= PIT_PER_PLAYER) return;
            if (currentPlayer.getSide() == PlayerSide.TOP && (pitId <= PIT_PER_PLAYER) || (pitId == PIT_PER_PLAYER * 2 + 1)) return;
        }

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

        if (!move.isGivesExtraTurn()) {
            currentPlayer = (currentPlayer == p1) ? p2 : p1;
        }

        updateTurnLabel(move.isGivesExtraTurn());

        if (mode == GameMode.PVC) {
            if (currentPlayer == p2) {
                startThinkingAnimation("Computer thinking");
                makeComputerMove();
            } else {
                view.enablePlayerButtons(PlayerSide.BOTTOM, getZeroButtons());
            }
        } else if (mode == GameMode.ANALYSIS) {
            view.enablePlayerButtons(currentPlayer.getSide(), getZeroButtons());
            startAnalysisForCurrentTurn();
        }
    }

    private void updateTurnLabel(boolean extraTurn) {
        if (mode == GameMode.PVC) {
            if (currentPlayer == p1) {
                view.bottomLabel.setText(extraTurn ? "Your turn again." : "Your turn.");
            }
        } else if (mode == GameMode.ANALYSIS) {
            String side = (currentPlayer.getSide() == PlayerSide.BOTTOM) ? "Bottom" : "Top";
            view.bottomLabel.setText(side + " player's turn.");
        }
    }

    private List<Integer> getZeroButtons() {
        List<Integer> zeroButtons = new ArrayList<>();
        for (int i = 0; i < PIT_PER_PLAYER; i++) {
            if (board[i] == 0) zeroButtons.add(i);
            if (board[i + PIT_PER_PLAYER + 1] == 0) zeroButtons.add(i + PIT_PER_PLAYER + 1);
        }
        return zeroButtons;
    }

    public void announceWinner() {
        isAnimationPlaying = false;
        stopThinkingAnimation();

        if (mode == GameMode.ANALYSIS && analysisThread != null) {
            nativeEngine.stopSearch();
            analysisThread.interrupt();
        }

        view.enablePlayerButtons(null, null);
        collectRemainingStones(board);

        view.buttonMap.forEach((id, pitButton) -> {
            pitButton.getButton().setText(String.valueOf(board[id]));
        });

        String message = "";
        GameResult result = checkGameState(board);

        if (mode == GameMode.PVC) {
            message = switch (result) {
                case BOTTOM_WON -> "CONGRATULATIONS! You WON! Score: " + board[PIT_PER_PLAYER] + " - " + board[PIT_PER_PLAYER * 2 + 1];
                case TOP_WON -> "GAME ENDED! Computer WON! Score: " + board[PIT_PER_PLAYER] + " - " + board[PIT_PER_PLAYER * 2 + 1];
                case DRAW -> "DRAW! Score: " + board[PIT_PER_PLAYER] + " - " + board[PIT_PER_PLAYER * 2 + 1];
                default -> "";
            };
        } else {
            message = switch (result) {
                case BOTTOM_WON -> "Bottom WON! Score: " + board[PIT_PER_PLAYER] + " - " + board[PIT_PER_PLAYER * 2 + 1];
                case TOP_WON -> "Top WON! Score: " + board[PIT_PER_PLAYER] + " - " + board[PIT_PER_PLAYER * 2 + 1];
                case DRAW -> "DRAW! Score: " + board[PIT_PER_PLAYER] + " - " + board[PIT_PER_PLAYER * 2 + 1];
                default -> "";
            };
        }

        view.bottomLabel.setText(message);
    }

    private GameResult checkGameState(int[] currentBoard) {
        boolean isBottomEmpty = true;
        boolean isTopEmpty = true;

        for (int i = 0; i < PIT_PER_PLAYER; i++) {
            if (currentBoard[i] > 0) { isBottomEmpty = false; break; }
        }
        for (int i = PIT_PER_PLAYER + 1; i < PIT_PER_PLAYER * 2 + 1; i++) {
            if (currentBoard[i] > 0) { isTopEmpty = false; break; }
        }

        if (isBottomEmpty || isTopEmpty) {
            int bottomScore = currentBoard[PIT_PER_PLAYER];
            int topScore = currentBoard[PIT_PER_PLAYER * 2 + 1];

            if (isBottomEmpty) {
                for (int i = PIT_PER_PLAYER + 1; i < PIT_PER_PLAYER * 2 + 1; i++) bottomScore += currentBoard[i];
            } else {
                for (int i = 0; i < PIT_PER_PLAYER; i++) topScore += currentBoard[i];
            }

            if (bottomScore > topScore) return GameResult.BOTTOM_WON;
            if (topScore > bottomScore) return GameResult.TOP_WON;
            return GameResult.DRAW;
        }
        return GameResult.CONTINUES;
    }

    private void collectRemainingStones(int[] currentBoard) {
        int bottomStones = 0, topStones = 0;
        for (int i = 0; i < PIT_PER_PLAYER; i++) { bottomStones += currentBoard[i]; currentBoard[i] = 0; }
        for (int i = PIT_PER_PLAYER + 1; i < PIT_PER_PLAYER * 2 + 1; i++) { topStones += currentBoard[i]; currentBoard[i] = 0; }
        if (bottomStones == 0) currentBoard[PIT_PER_PLAYER] += topStones;
        if (topStones == 0) currentBoard[PIT_PER_PLAYER * 2 + 1] += bottomStones;
    }

    private void makeComputerMove() {
        Task<Integer> computerTask = new Task<>() {
            @Override
            protected Integer call() {
                boolean isTop = (p2.getSide() == PlayerSide.TOP);
                return nativeEngine.findBestMove(board, p2.getDepth(), isTop)[0];
            }
        };

        computerTask.setOnSucceeded(e -> {
            int bestMove = computerTask.getValue();
            stopThinkingAnimation();
            view.bottomLabel.setText("Computer chose pit number " + bestMove + "!");
            MoveResult move = moveHandler.move(board, bestMove, PlayerSide.TOP);
            executeMoveSequence(move);
        });

        computerTask.setOnFailed(e -> {
            view.bottomLabel.setText("ERROR! Computer failed to calculate move!");
            e.getSource().getException().printStackTrace();
            isAnimationPlaying = false;
        });

        Thread thread = new Thread(computerTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void startAnalysisForCurrentTurn() {
        if (!view.isEngineEnabled()) {
            view.bottomLabel.setText((currentPlayer.getSide() == PlayerSide.BOTTOM ? "Bottom" : "Top") + " player's turn. Analysis is off.");
            view.enablePlayerButtons(currentPlayer.getSide(), getZeroButtons());
            return;
        }

        int depth = view.getEngineDepth();
        startThinkingAnimation("Analyzing moves (Depth: " + depth + ")");
        view.enablePlayerButtons(null, null);

        Task<Map<Integer, Integer>> analysisTask = new Task<>() {
            @Override
            protected Map<Integer, Integer> call() throws Exception {
                Map<Integer, Integer> evaluations = new HashMap<>();

                int startIdx = (currentPlayer.getSide() == PlayerSide.BOTTOM) ? 0 : PIT_PER_PLAYER + 1;
                int endIdx = (currentPlayer.getSide() == PlayerSide.BOTTOM) ? PIT_PER_PLAYER - 1 : PIT_PER_PLAYER * 2;

                for (int i = startIdx; i <= endIdx; i++) {
                    if (Thread.currentThread().isInterrupted()) break;
                    System.out.println("Analysis for current turn: " + i);

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

        analysisThread = new Thread(analysisTask);
        analysisThread.setDaemon(true);
        analysisThread.start();
    }

    public void restartAnalysis() {
        if (analysisThread != null && analysisThread.isAlive()) {
            nativeEngine.stopSearch();
            analysisThread.interrupt();
            stopThinkingAnimation();
        }

        if (view.isEngineEnabled()) {
            startAnalysisForCurrentTurn();
        } else {
            view.bottomLabel.setText("Analysis is off.");
            view.enablePlayerButtons(currentPlayer.getSide(), getZeroButtons());
        }
    }

    private void startThinkingAnimation(String baseText) {
        stopThinkingAnimation();
        thinkingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> view.bottomLabel.setText(baseText + ".")),
                new KeyFrame(Duration.seconds(1.0), e -> view.bottomLabel.setText(baseText + "..")),
                new KeyFrame(Duration.seconds(1.5), e -> view.bottomLabel.setText(baseText + "..."))
        );
        thinkingTimeline.setCycleCount(Animation.INDEFINITE);
        thinkingTimeline.play();
    }

    private void stopThinkingAnimation() {
        if (thinkingTimeline != null) thinkingTimeline.stop();
    }
}