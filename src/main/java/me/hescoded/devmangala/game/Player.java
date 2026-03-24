package me.hescoded.devmangala.game;

import me.hescoded.devmangala.variables.PlayerSide;
import me.hescoded.devmangala.variables.PlayerType;

public class Player {
    private final PlayerSide side;
    private final PlayerType type;
    private final int depth;

    public Player(PlayerSide side, PlayerType type, int depth) {
        this.side = side;
        this.type = type;
        this.depth = depth;
    }

    public PlayerSide getSide() {
        return side;
    }

    public PlayerType getType() {
        return type;
    }

    public int getDepth() {
        return depth;
    }
}
