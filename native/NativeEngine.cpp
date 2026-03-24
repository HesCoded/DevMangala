#include <jni.h>
#include <iostream>
#include <vector>
#include <algorithm>
#include <chrono>
#include <fstream>
#include <unordered_map>
#include <cstdint>
#include <omp.h> 

using namespace std;

unordered_map<uint64_t, int8_t> tablebase;
bool tablebase_loaded = false;

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

struct TTEntry {
    uint64_t hash = 0;
    int depth = -1;
    int score = 0;
    int flag = -1;
    int bestMove = -1;
};

const int TABLE_SIZE = 1 << 22; 
TTEntry tt[TABLE_SIZE];
uint64_t zobristTable[14][49];
uint64_t zobristSideToMove;

long long nodeCount = 0;
long long tb_hits = 0; 
long long total_lookups = 0;
long long tt_hits = 0;

void initZobrist() {
    static bool initialized = false;
    if (initialized) return;
    srand(12345);
    for (int i = 0; i < 14; i++) {
        for (int j = 0; j < 49; j++) {
            zobristTable[i][j] = ((uint64_t)rand() << 32) | (uint64_t)rand();
        }
    }
    zobristSideToMove = ((uint64_t)rand() << 32) | (uint64_t)rand();
    initialized = true;
}

uint64_t calcHash(const uint8_t board[14], bool isTopPlayer) {
    uint64_t h = 0;
    for (int i = 0; i < 14; i++) {
        uint8_t stones = board[i];
        if (stones > 0 && stones <= 48) h ^= zobristTable[i][stones];
    }
    if (isTopPlayer) h ^= zobristSideToMove;
    return h;
}

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
    bool isBottomEmpty = true;
    for (int i = 0; i < 6; i++) { if (board[i] > 0) { isBottomEmpty = false; break; } }
    bool isTopEmpty = true;
    for (int i = 7; i < 13; i++) { if (board[i] > 0) { isTopEmpty = false; break; } }

    if (isBottomEmpty || isTopEmpty) {
        int bScore = board[6];
        int tScore = board[13];
        if (isBottomEmpty) { for (int i = 7; i < 13; i++) bScore += board[i]; }
        else { for (int i = 0; i < 6; i++) tScore += board[i]; }

        if (bScore > tScore) return 1; // Bottom wins
        if (tScore > bScore) return 2; // Top wins
        return 3; // Draw
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
        if (gameState == 1) return 10000 + diff;
        if (gameState == 2) return -10000 + diff;
        return diff;
    }
    return bScore - tScore;
}

// This function could be modified to prioritize moves that result in pieces being captured or causes an extra move; I'm keeping it simple for now.
void sortPits(const uint8_t board[14], int resultPits[6], bool isTopPlayer, int ttBestMove) {
    int offset = isTopPlayer ? 7 : 0;
    int scores[6];
    for (int i = 0; i < 6; i++) {
        int idx = offset + i;
        resultPits[i] = idx;
        if (board[idx] == 0) scores[i] = -1000;
        else if (idx == ttBestMove) scores[i] = 1000;
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
    // Tablebase Check
    int active_stones = 0;
    for (int i = 0; i < 14; i++) if(i != 6 && i != 13) active_stones += board[i];

    if (active_stones <= 15 && tablebase_loaded) {
        uint8_t tb_pits[12];
        for (int i = 0; i < 6; i++) {
            tb_pits[i] = board[i];
            tb_pits[i + 6] = board[i + 7];
        }
        uint64_t tb_key = encode_tb(tb_pits, isTopPlayer);
        if (tablebase.count(tb_key)) {
            #pragma omp atomic
            tb_hits++;
            int future_diff = tablebase[tb_key]; // Bottom - Top
            int final_diff = (board[6] - board[13]) + future_diff;
            return (final_diff > 0) ? (10000 + final_diff) : (final_diff < 0 ? -10000 + final_diff : 0); // "return final_diff;" is also possible.
        }
    }

    // TT Check
    uint64_t hash = calcHash(board, isTopPlayer);
    int ttIndex = hash & (TABLE_SIZE - 1);
    #pragma omp atomic
    total_lookups++;
    if (tt[ttIndex].hash == hash && tt[ttIndex].depth >= depth) {
        #pragma omp atomic
        tt_hits++;
        if (tt[ttIndex].flag == 0) return tt[ttIndex].score;
        if (tt[ttIndex].flag == 1 && tt[ttIndex].score <= alpha) return tt[ttIndex].score;
        if (tt[ttIndex].flag == 2 && tt[ttIndex].score >= beta) return tt[ttIndex].score;
    }

    int state = checkGameState(board);
    if (depth <= 0 || state != 0) return evaluate(board, state);

    #pragma omp atomic
    nodeCount++;

    int bestMoveAtNode = -1;
    int sortedPits[6];
    sortPits(board, sortedPits, isTopPlayer, (tt[ttIndex].hash == hash) ? tt[ttIndex].bestMove : -1);

    if (!isTopPlayer) { // Maximizing (Bottom)
        int maxEval = -999999;
        for (int i = 0; i < 6; i++) {
            int pit = sortedPits[i];
            if (board[pit] == 0) continue;
            uint8_t nextBoard[14];
            copy(board, board + 14, nextBoard);
            bool extra = makeMove(nextBoard, pit, false);
            int eval = minimax(nextBoard, depth - 1, extra ? false : true, alpha, beta);
            if (eval > maxEval) { maxEval = eval; bestMoveAtNode = pit; }
            alpha = max(alpha, eval);
            if (beta <= alpha) break;
        }
        int flag = (maxEval <= alpha) ? 1 : (maxEval >= beta) ? 2 : 0;
        tt[ttIndex] = {hash, depth, maxEval, flag, bestMoveAtNode};
        return maxEval;
    } else { // Minimizing (Top)
        int minEval = 999999;
        for (int i = 0; i < 6; i++) {
            int pit = sortedPits[i];
            if (board[pit] == 0) continue;
            uint8_t nextBoard[14];
            copy(board, board + 14, nextBoard);
            bool extra = makeMove(nextBoard, pit, true);
            int eval = minimax(nextBoard, depth - 1, extra ? true : false, alpha, beta);
            if (eval < minEval) { minEval = eval; bestMoveAtNode = pit; }
            beta = min(beta, eval);
            if (beta <= alpha) break;
        }
        int flag = (minEval <= alpha) ? 1 : (minEval >= beta) ? 2 : 0;
        tt[ttIndex] = {hash, depth, minEval, flag, bestMoveAtNode};
        return minEval;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_me_hescoded_devmangala_game_NativeEngine_findBestMove(JNIEnv *env, jobject obj, jintArray jBoard, jint targetDepth, jboolean isTopPlayer) {
    loadTablebase(); 
    initZobrist();
    for (int i = 0; i < TABLE_SIZE; i++) tt[i].hash = 0; 
    nodeCount = 0; tb_hits = 0;

    jint *cBoard = env->GetIntArrayElements(jBoard, NULL);
    uint8_t startBoard[14];
    for (int i = 0; i < 14; i++) startBoard[i] = (uint8_t)cBoard[i];
    env->ReleaseIntArrayElements(jBoard, cBoard, JNI_ABORT);

    int overallBestMove = -1;
    for (int d = 1; d <= targetDepth; d++) {
        auto start = chrono::high_resolution_clock::now();
        int bestScore = isTopPlayer ? 999999 : -999999;
        int moveThisDepth = -1;

        #pragma omp parallel for schedule(dynamic)
        for (int i = 0; i < 6; i++) {
            int pit = (isTopPlayer ? 7 : 0) + i;
            if (startBoard[pit] == 0) continue;

            uint8_t nextBoard[14];
            copy(startBoard, startBoard + 14, nextBoard);
            bool extra = makeMove(nextBoard, pit, isTopPlayer);
            int score = minimax(nextBoard, d - 1, extra ? isTopPlayer : !isTopPlayer, -999999, 999999);

            #pragma omp critical
            {
                if (isTopPlayer) {
                    if (score < bestScore) { bestScore = score; moveThisDepth = pit; }
                } else {
                    if (score > bestScore) { bestScore = score; moveThisDepth = pit; }
                }
            }
        }
        overallBestMove = moveThisDepth;
        auto end = chrono::high_resolution_clock::now();
        cout << "Depth: " << d << " | Best Pit: " << overallBestMove << " | Score: " << bestScore 
             << " | Nodes: " << nodeCount << " | Time: " << chrono::duration<double>(end-start).count() << "s" 
             << " | Zobrist Rate: " << ((float)tt_hits / total_lookups) * 100 << "%" << " | TT-Hits: " << tt_hits << endl;
    }
    return overallBestMove;
}