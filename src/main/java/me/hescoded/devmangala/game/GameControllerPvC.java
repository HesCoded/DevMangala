package me.hescoded.devmangala.game;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.util.Duration;
import me.hescoded.devmangala.ui.BoardView;
import me.hescoded.devmangala.variables.GameResult;
import me.hescoded.devmangala.variables.PlayerSide;

public class GameControllerPvC {
    private final Player p1, p2;
    private int[] board;
    private Player currentPlayer;
    private BoardView view;
    private MoveHandler moveHandler;
    private boolean isAnimationPlaying = false;
    private Timeline thinkingTimeline;
    private NativeEngine nativeEngine;

    public GameControllerPvC(Player p1, Player p2, PlayerSide firstPlayer, BoardView view) {
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
        view.enablePlayerButtons(currentPlayer.getSide());
    }

    public void onPitClicked(int pitId) {
        if (currentPlayer.getSide() != PlayerSide.BOTTOM || isAnimationPlaying || board[pitId] == 0) return;
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
                startThinkingAnimation();
                makeComputerMove();
            } else {
                view.bottomLabel.setText("Your turn again.");
            }
        } else {
            currentPlayer = (currentPlayer == p1) ? p2 : p1;
            if (currentPlayer == p2) {
                startThinkingAnimation();
                makeComputerMove();
            } else {
                view.bottomLabel.setText("Your turn.");
            }
        }
    }

    public void announceWinner() {
        isAnimationPlaying = false;
        stopThinkingAnimation();
        view.enablePlayerButtons(null);
        collectRemainingStones(board);
        view.buttonMap.forEach((id, pitButton) -> {
            pitButton.getButton().setText(String.valueOf(board[id]));
        });

        String message = switch (checkGameState(board)) {
            case BOTTOM_WON -> "CONGRATULATIONS! You WON! Score: " + board[6] + " - " + board[13];
            case TOP_WON -> "GAME ENDED! Computer WON! Score: " + board[6] + " - " + board[13];
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

    private void makeComputerMove() {
        Task<Integer> computerTask = new Task<>() {
            @Override
            protected Integer call() {
                boolean isTop = (p2.getSide() == PlayerSide.TOP);
                return nativeEngine.findBestMove(board, p2.getDepth(), isTop);
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

    private void startThinkingAnimation() {
        thinkingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> view.bottomLabel.setText("Computer thinking.")),
                new KeyFrame(Duration.seconds(1.0), e -> view.bottomLabel.setText("Computer thinking..")),
                new KeyFrame(Duration.seconds(1.5), e -> view.bottomLabel.setText("Computer thinking..."))
        );
        thinkingTimeline.setCycleCount(Animation.INDEFINITE);
        thinkingTimeline.play();
    }

    private void stopThinkingAnimation() {
        if (thinkingTimeline != null) thinkingTimeline.stop();
    }
}