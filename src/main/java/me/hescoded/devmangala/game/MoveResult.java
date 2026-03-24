package me.hescoded.devmangala.game;

import java.util.List;

public class MoveResult {
    private final boolean isLegalMove;
    private final boolean isGivesExtraTurn;
    private List<Integer> path;

    public MoveResult(boolean isLegalMove, boolean isGivesExtraTurn, List<Integer> path) {
        this.isLegalMove = isLegalMove;
        this.isGivesExtraTurn = isGivesExtraTurn;
        this.path = path;
    }

    public boolean isLegalMove() {
        return isLegalMove;
    }

    public boolean isGivesExtraTurn() {
        return isGivesExtraTurn;
    }

    public List<Integer> getPath() {
        return path;
    }
}
