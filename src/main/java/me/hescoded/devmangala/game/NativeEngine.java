package me.hescoded.devmangala.game;

public class NativeEngine {
    static {
        String libPath = System.getProperty("user.dir") + "/lib/mn88engine.dll";
        System.load(libPath);
    }
    public native int[] findBestMove(int[] board, int targetDepth, boolean isTopPlayer);
    public native void stopSearch();
    public native void startSearch();
}
