package me.hescoded.devmangala.game;

import me.hescoded.devmangala.variables.PlayerSide;

import java.util.ArrayList;
import java.util.List;

public class MoveHandler {
    public MoveResult move(int[] board, int index, PlayerSide side) {
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
                if (index == 14) index = 0;
                if (side == PlayerSide.BOTTOM && index == 13) continue;
                if (side == PlayerSide.TOP && index == 6) continue;
                board[index] = board[index] + 1;
                path.add(index);
                stones--;
            }
        }

        if ((side == PlayerSide.BOTTOM && index == 6) || (side == PlayerSide.TOP && index == 13)) return new MoveResult(true, true, path);

        if (side == PlayerSide.BOTTOM && (index < 6) && (board[index] == 1)) {
            int oppIndex = 12 - index;
            if (board[oppIndex] > 0) {
                board[6] = board[6] + board[index] + board[oppIndex];
                board[index] = 0;
                board[oppIndex] = 0;
            }
        }

        if (side == PlayerSide.TOP && (index < 13) && (index > 6) && (board[index] == 1)) {
            int oppIndex = 12 - index;
            if (board[oppIndex] > 0) {
                board[13] = board[13] + board[index] + board[oppIndex];
                board[index] = 0;
                board[oppIndex] = 0;
            }
        }

        if (side == PlayerSide.BOTTOM && (index < 13) && (index > 6) && (board[index] % 2 == 0)) {
            board[6] = board[6] + board[index];
            board[index] = 0;
        }

        if (side == PlayerSide.TOP && (index < 6) && (board[index] % 2 == 0)) {
            board[13] = board[13] + board[index];
            board[index] = 0;
        }

        return new MoveResult(true, false, path);
    }

    public MoveResult moveForEvaluation(int[] board, int index, PlayerSide side) {
        if (board[index] == 0) return new MoveResult(false, false, null);

        int stones = board[index];

        if (stones == 1) {
            board[index] = 0;
            index++;
            board[index] = board[index] + 1;
        } else {
            board[index] = 1;
            stones--;
            while (stones > 0) {
                index++;
                if (index == 14) index = 0;
                if (side == PlayerSide.BOTTOM && index == 13) continue;
                if (side == PlayerSide.TOP && index == 6) continue;
                board[index] = board[index] + 1;
                stones--;
            }
        }

        if ((side == PlayerSide.BOTTOM && index == 6) || (side == PlayerSide.TOP && index == 13)) return new MoveResult(true, true, null);

        if (side == PlayerSide.BOTTOM && (index < 6) && (board[index] == 1)) {
            int oppIndex = 12 - index;
            if (board[oppIndex] > 0) {
                board[6] = board[6] + board[index] + board[oppIndex];
                board[index] = 0;
                board[oppIndex] = 0;
            }
        }

        if (side == PlayerSide.TOP && (index < 13) && (index > 6) && (board[index] == 1)) {
            int oppIndex = 12 - index;
            if (board[oppIndex] > 0) {
                board[13] = board[13] + board[index] + board[oppIndex];
                board[index] = 0;
                board[oppIndex] = 0;
            }
        }

        if (side == PlayerSide.BOTTOM && (index < 13) && (index > 6) && (board[index] % 2 == 0)) {
            board[6] = board[6] + board[index];
            board[index] = 0;
        }

        if (side == PlayerSide.TOP && (index < 6) && (board[index] % 2 == 0)) {
            board[13] = board[13] + board[index];
            board[index] = 0;
        }

        return new MoveResult(true, false, null);
    }
}