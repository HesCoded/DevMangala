package me.hescoded.devmangala.game;

import me.hescoded.devmangala.variables.PlayerSide;

public class Player {
    private final PlayerSide side;
    private final int depth;

    public Player(PlayerSide side, int depth) {
        this.side = side;
        this.depth = depth;
    }

    public PlayerSide getSide() {
        return side;
    }

    public int getDepth() {
        return depth;
    }
}
