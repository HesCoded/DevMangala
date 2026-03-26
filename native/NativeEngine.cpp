#include <jni.h>
#include <iostream>
#include <vector>
#include <algorithm>
#include <chrono>
#include <fstream>
#include <unordered_map>
#include <cstdint>
#include <atomic>

using namespace std;

unordered_map<uint64_t, int8_t> tablebase;
bool tablebase_loaded = false;
std::atomic<bool> stopSearch(false);

// We have 12 pits and 5 bits for every pits. 61th bit is for turn player, if turn player is top side, it is 1, else 0.
uint64_t encode_tb(uint8_t pits[12], bool isTopPlayer) {
    uint64_t key = 0;
    for (int i = 0; i < 12; ++i) key |= ((uint64_t)pits[i] << (i * 5));
    if (isTopPlayer) key |= (1ULL << 60);
    return key;
}

void loadTablebase() {
    if (tablebase_loaded) return;
    ifstream infile("data/mangala_15stones.bin", ios::binary);
    if (!infile) {
        cout << "INFO: Tablebase file not found! Only the engine will be used for calculations." << endl;
        return;
    }
    uint64_t size;
    infile.read(reinterpret_cast<char*>(&size), sizeof(size));
    for (uint64_t i = 0; i < size; ++i) {
        uint64_t key;
        int8_t val;
        infile.read(reinterpret_cast<char*>(&key), sizeof(key));
        infile.read(reinterpret_cast<char*>(&val), sizeof(val));
        tablebase[key] = val;
    }
    infile.close();
    tablebase_loaded = true;
    cout << "--- Tablebase loaded: " << size << " positions were added to RAM ---" << endl;
}

// Killer move heuristic: stores best moves at each depth
int killerMoves[64][2]; // [depth][0 or 1]
long long nodeCount = 0;

bool makeMove(uint8_t board[14], int index, bool isTopPlayer) {
    uint8_t stones = board[index];
    if (stones == 0) return false;

    int ownStore = isTopPlayer ? 13 : 6;
    int oppStore = isTopPlayer ? 6 : 13;

    board[index] = 0;
    int curr = index;

    if (stones == 1) {
        curr = (curr + 1) % 14;
        board[curr]++;
    } else {
        board[index] = 1;
        stones--;
        while (stones > 0) {
            curr = (curr + 1) % 14;
            if (curr == oppStore) continue;
            board[curr]++;
            stones--;
        }
    }

    if (curr == ownStore) return true;

    bool isOppSide = isTopPlayer ? (curr >= 0 && curr <= 5) : (curr >= 7 && curr <= 12);
    if (isOppSide && board[curr] % 2 == 0) {
        board[ownStore] += board[curr];
        board[curr] = 0;
    }

    bool isOwnSide = isTopPlayer ? (curr >= 7 && curr <= 12) : (curr >= 0 && curr <= 5);
    if (isOwnSide && board[curr] == 1) {
        int oppIndex = 12 - curr;
        if (board[oppIndex] > 0) {
            board[ownStore] += (board[curr] + board[oppIndex]);
            board[curr] = 0;
            board[oppIndex] = 0;
        }
    }

    return false;
}

int checkGameState(uint8_t board[14]) {
    bool bottomEmpty = true;
    for (int i = 0; i < 6; i++) { if (board[i] > 0) { bottomEmpty = false; break; } }

    if (bottomEmpty) {
        int bScore = board[6];
        int tScore = board[13];
        for (int i = 7; i < 13; i++) tScore += board[i];
        if (bScore > tScore) return 1;
        if (tScore > bScore) return 2;
        return 3;
    }

    bool topEmpty = true;
    for (int i = 7; i < 13; i++) { if (board[i] > 0) { topEmpty = false; break; } }

    if (topEmpty) {
        int bScore = board[6];
        int tScore = board[13];
        for (int i = 0; i < 6; i++) bScore += board[i];
        if (bScore > tScore) return 1;
        if (tScore > bScore) return 2;
        return 3;
    }

    return 0;
}

int evaluate(uint8_t board[14], int gameState) {
    int bScore = board[6];
    int tScore = board[13];

    if (gameState != 0) {
        bool isBottomEmpty = true;
        for (int i = 0; i < 6; i++) { if (board[i] > 0) { isBottomEmpty = false; break; } }
        if (isBottomEmpty) { for (int i = 7; i < 13; i++) bScore += board[i]; }
        else { for (int i = 0; i < 6; i++) tScore += board[i]; }

        int diff = bScore - tScore;
        // if (gameState == 1) return 10000 + diff;
        // if (gameState == 2) return -10000 + diff;
        return diff;
    }
    return bScore - tScore;
}

void sortPits(const uint8_t board[14], int resultPits[6], bool isTopPlayer, int depth) {
    int offset = isTopPlayer ? 7 : 0;
    int scores[6];
    for (int i = 0; i < 6; i++) {
        int idx = offset + i;
        resultPits[i] = idx;
        if (board[idx] == 0) scores[i] = -1000;
        else if (idx == killerMoves[depth][0] || idx == killerMoves[depth][1]) scores[i] = 5000;
        else scores[i] = board[idx];
    }
    for (int i = 0; i < 5; i++) {
        for (int j = i + 1; j < 6; j++) {
            if (scores[j] > scores[i]) {
                swap(scores[i], scores[j]);
                swap(resultPits[i], resultPits[j]);
            }
        }
    }
}

int minimax(uint8_t board[14], int depth, bool isTopPlayer, int alpha, int beta) {
    if (stopSearch) {
        cout << "INFO: Search stopped at depth " << depth << " due to stopSearch flag." << endl;
        return 0;
    } 
    
    int state = checkGameState(board);
    if (state != 0) return evaluate(board, state);

    int active_stones = 48 - board[6] - board[13];
    if (active_stones <= 15 && tablebase_loaded) {
        uint8_t tb_pits[12];
        for (int i = 0; i < 6; i++) {
            tb_pits[i] = board[i];
            tb_pits[i+6] = board[i+7];
        }

        uint64_t tb_key = encode_tb(tb_pits, isTopPlayer);
        auto it = tablebase.find(tb_key);
        if (it != tablebase.end()) {
            return it->second;
        }
    }

    if (depth <= 0) return evaluate(board, 0);

    nodeCount++;
    int sortedPits[6];
    sortPits(board, sortedPits, isTopPlayer, depth);

    if (!isTopPlayer) {
        int maxEval = -999999;
        for (int i = 0; i < 6; i++) {
            int pit = sortedPits[i];
            if (board[pit] == 0) continue;

            uint8_t nextBoard[14];
            copy(board, board + 14, nextBoard);
            bool extra = makeMove(nextBoard, pit, false);

            int eval = minimax(nextBoard, depth - 1, extra ? false : true, alpha, beta);
            if (eval > maxEval) { maxEval = eval; }
            alpha = max(alpha, eval);
            if (beta <= alpha) {
                if (killerMoves[depth][0] != pit) {
                    killerMoves[depth][1] = killerMoves[depth][0];
                    killerMoves[depth][0] = pit;
                }
                break;
            }
        }
        return maxEval;
    } else {
        int minEval = 999999;
        for (int i = 0; i < 6; i++) {
            int pit = sortedPits[i];
            if (board[pit] == 0) continue;

            uint8_t nextBoard[14];
            copy(board, board + 14, nextBoard);
            bool extra = makeMove(nextBoard, pit, true);

            int eval = minimax(nextBoard, depth - 1, extra ? true : false, alpha, beta);
            if (eval < minEval) { minEval = eval; }
            beta = min(beta, eval);
            if (beta <= alpha) {
                if (killerMoves[depth][0] != pit) {
                    killerMoves[depth][1] = killerMoves[depth][0];
                    killerMoves[depth][0] = pit;
                }
                break;
            }
        }
        return minEval;
    }
}

extern "C" JNIEXPORT jintArray JNICALL
Java_me_hescoded_devmangala_game_NativeEngine_findBestMove(JNIEnv *env, jobject obj, jintArray jBoard, jint targetDepth, jboolean isTopPlayer) {
    loadTablebase();

    for (int i = 0; i < 64; i++) {
        killerMoves[i][0] = -1;
        killerMoves[i][1] = -1;
    }

    nodeCount = 0;

    jint *cBoard = env->GetIntArrayElements(jBoard, NULL);
    uint8_t startBoard[14];
    for (int i = 0; i < 14; i++) startBoard[i] = (uint8_t)cBoard[i];
    env->ReleaseIntArrayElements(jBoard, cBoard, JNI_ABORT);

    int overallBestMove = -1;
    int finalBestScore = 0;
    for (int d = 1; d <= targetDepth; d++) {
        auto start = chrono::high_resolution_clock::now();
        int bestScore = isTopPlayer ? 999999 : -999999;
        int moveThisDepth = -1;

        int alpha = -999999;
        int beta = 999999;
        for (int i = 0; i < 6; i++) {
            int pit = (isTopPlayer ? 7 : 0) + i;
            if (startBoard[pit] == 0) continue;

            uint8_t nextBoard[14];
            copy(startBoard, startBoard + 14, nextBoard);
            bool extra = makeMove(nextBoard, pit, isTopPlayer);

            int score = minimax(nextBoard, d - 1, extra ? isTopPlayer : !isTopPlayer, alpha, beta);

            if (isTopPlayer) {
                if (score < bestScore) { bestScore = score; moveThisDepth = pit; }
                beta = min(beta, bestScore);
            } else {
                if (score > bestScore) { bestScore = score; moveThisDepth = pit; }
                alpha = max(alpha, bestScore);
            }
        }
        overallBestMove = moveThisDepth;
        finalBestScore = bestScore;
        auto end = chrono::high_resolution_clock::now();
        cout << "Depth: " << d << " | Best Pit: " << overallBestMove << " | Score: " << bestScore
             << " | Nodes: " << nodeCount << " | Time: " << chrono::duration<double>(end-start).count() << "s"
             << endl;
    }
    jintArray result = env->NewIntArray(2);
    jint elements[2] = { overallBestMove, finalBestScore };
    env->SetIntArrayRegion(result, 0, 2, elements);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_me_hescoded_devmangala_game_NativeEngine_stopSearch(JNIEnv *env, jobject obj) {
    stopSearch = true;
    cout << "INFO: Search stop requested by Java side." << endl;
}

extern "C" JNIEXPORT void JNICALL
Java_me_hescoded_devmangala_game_NativeEngine_startSearch(JNIEnv *env, jobject obj) {
    stopSearch = false;
    cout << "INFO: Search started by Java side." << endl;
}