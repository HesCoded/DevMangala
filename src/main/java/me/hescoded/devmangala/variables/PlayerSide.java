package me.hescoded.devmangala.variables;

public enum PlayerSide {
    BOTTOM(1000), TOP(-1000);
    private final int value;
    PlayerSide(int value) {
        this.value = value;
    }

    public int getInstantWinValue() {
        return value;
    }
}
