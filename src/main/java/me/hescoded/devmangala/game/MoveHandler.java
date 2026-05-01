package me.hescoded.devmangala.game;

import me.hescoded.devmangala.variables.PlayerSide;

import java.util.ArrayList;
import java.util.List;

public class MoveHandler {
    public MoveResult move(int[] board, int index, PlayerSide side) {
        int rightStoreId = (board.length / 2) - 1;
        int leftStoreId = board.length - 1;

        if (board[index] == 0) return new MoveResult(false, false, null);

        int stones = board[index];
        List<Integer> path = new ArrayList<>();

        if (stones == 1) {
            board[index] = 0;
            path.add(index);
            index++;
            board[index] = board[index] + 1;
            path.add(index);
        } else {
            board[index] = 1;
            path.add(index);
            stones--;
            while (stones > 0) {
                index++;
                if (index == board.length) index = 0;
                if (side == PlayerSide.BOTTOM && index == leftStoreId) continue;
                if (side == PlayerSide.TOP && index == rightStoreId) continue;
                board[index] = board[index] + 1;
                path.add(index);
                stones--;
            }
        }

        if ((side == PlayerSide.BOTTOM && index == rightStoreId) || (side == PlayerSide.TOP && index == leftStoreId)) return new MoveResult(true, true, path);

        if (side == PlayerSide.BOTTOM && (index < rightStoreId) && (board[index] == 1)) {
            int oppIndex = leftStoreId - 1 - index;
            if (board[oppIndex] > 0) {
                board[rightStoreId] = board[rightStoreId] + board[index] + board[oppIndex];
                board[index] = 0;
                board[oppIndex] = 0;
            }
        }

        if (side == PlayerSide.TOP && (index < leftStoreId) && (index > rightStoreId) && (board[index] == 1)) {
            int oppIndex = leftStoreId - 1 - index;
            if (board[oppIndex] > 0) {
                board[leftStoreId] = board[leftStoreId] + board[index] + board[oppIndex];
                board[index] = 0;
                board[oppIndex] = 0;
            }
        }

        if (side == PlayerSide.BOTTOM && (index < leftStoreId) && (index > rightStoreId) && (board[index] % 2 == 0)) {
            board[rightStoreId] = board[rightStoreId] + board[index];
            board[index] = 0;
        }

        if (side == PlayerSide.TOP && (index < rightStoreId) && (board[index] % 2 == 0)) {
            board[leftStoreId] = board[leftStoreId] + board[index];
            board[index] = 0;
        }

        return new MoveResult(true, false, path);
    }
}