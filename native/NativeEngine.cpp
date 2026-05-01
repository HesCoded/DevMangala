#include <jni.h>
#include <iostream>
#include <vector>
#include <algorithm>
#include <chrono>
#include <cstdint>
#include <atomic>

using namespace std;

std::atomic<bool> stopSearch(false);

// Killer move heuristic: stores best moves at each depth
int killerMoves[64][2]; // [depth][0 or 1]
long long nodeCount = 0;

bool makeMove(vector<uint8_t>& board, int index, bool isTopPlayer, int pitsPerPlayer) {
    uint8_t stones = board[index];
    if (stones == 0) return false;

    int boardSize = pitsPerPlayer * 2 + 2;
    int ownStore = isTopPlayer ? boardSize - 1 : pitsPerPlayer;
    int oppStore = isTopPlayer ? pitsPerPlayer : boardSize - 1;

    board[index] = 0;
    int curr = index;

    if (stones == 1) {
        curr = (curr + 1) % boardSize;
        board[curr]++;
    } else {
        board[index] = 1;
        stones--;
        while (stones > 0) {
            curr = (curr + 1) % boardSize;
            if (curr == oppStore) continue;
            board[curr]++;
            stones--;
        }
    }

    if (curr == ownStore) return true;

    bool isOppSide = isTopPlayer ? (curr >= 0 && curr < pitsPerPlayer) : (curr > pitsPerPlayer && curr < boardSize - 1);
    if (isOppSide && board[curr] % 2 == 0) {
        board[ownStore] += board[curr];
        board[curr] = 0;
    }

    bool isOwnSide = isTopPlayer ? (curr > pitsPerPlayer && curr < boardSize - 1) : (curr >= 0 && curr < pitsPerPlayer);
    if (isOwnSide && board[curr] == 1) {
        int oppIndex = (pitsPerPlayer * 2) - curr;
        if (board[oppIndex] > 0) {
            board[ownStore] += (board[curr] + board[oppIndex]);
            board[curr] = 0;
            board[oppIndex] = 0;
        }
    }

    return false;
}

int checkGameState(const vector<uint8_t>& board, int pitsPerPlayer) {
    int boardSize = pitsPerPlayer * 2 + 2;
    int bottomStore = pitsPerPlayer;
    int topStore = boardSize - 1;

    bool bottomEmpty = true;
    for (int i = 0; i < pitsPerPlayer; i++) { 
        if (board[i] > 0) { bottomEmpty = false; break; } 
    }

    if (bottomEmpty) {
        int bScore = board[bottomStore];
        int tScore = board[topStore];
        for (int i = pitsPerPlayer + 1; i < topStore; i++) tScore += board[i];
        if (bScore > tScore) return 1;
        if (tScore > bScore) return 2;
        return 3;
    }

    bool topEmpty = true;
    for (int i = pitsPerPlayer + 1; i < topStore; i++) { 
        if (board[i] > 0) { topEmpty = false; break; } 
    }

    if (topEmpty) {
        int bScore = board[bottomStore];
        int tScore = board[topStore];
        for (int i = 0; i < pitsPerPlayer; i++) bScore += board[i];
        if (bScore > tScore) return 1;
        if (tScore > bScore) return 2;
        return 3;
    }

    return 0;
}

int evaluate(const vector<uint8_t>& board, int gameState, int pitsPerPlayer) {
    int bottomStore = pitsPerPlayer;
    int topStore = pitsPerPlayer * 2 + 1;
    
    int bScore = board[bottomStore];
    int tScore = board[topStore];

    if (gameState != 0) {
        bool isBottomEmpty = true;
        for (int i = 0; i < pitsPerPlayer; i++) { 
            if (board[i] > 0) { isBottomEmpty = false; break; } 
        }
        
        if (isBottomEmpty) { 
            for (int i = pitsPerPlayer + 1; i < topStore; i++) bScore += board[i]; 
        } else { 
            for (int i = 0; i < pitsPerPlayer; i++) tScore += board[i]; 
        }

        return bScore - tScore;
    }
    return bScore - tScore;
}

void sortPits(const vector<uint8_t>& board, vector<int>& resultPits, bool isTopPlayer, int depth, int pitsPerPlayer) {
    int offset = isTopPlayer ? pitsPerPlayer + 1 : 0;
    vector<int> scores(pitsPerPlayer);
    
    resultPits.resize(pitsPerPlayer);
    
    for (int i = 0; i < pitsPerPlayer; i++) {
        int idx = offset + i;
        resultPits[i] = idx;
        if (board[idx] == 0) scores[i] = -1000;
        else if (idx == killerMoves[depth][0] || idx == killerMoves[depth][1]) scores[i] = 5000;
        else scores[i] = board[idx];
    }
    
    for (int i = 0; i < pitsPerPlayer - 1; i++) {
        for (int j = i + 1; j < pitsPerPlayer; j++) {
            if (scores[j] > scores[i]) {
                swap(scores[i], scores[j]);
                swap(resultPits[i], resultPits[j]);
            }
        }
    }
}

int minimax(vector<uint8_t>& board, int depth, bool isTopPlayer, int alpha, int beta, int pitsPerPlayer) {
    if (stopSearch) {
        return 0;
    } 
    
    int state = checkGameState(board, pitsPerPlayer);
    if (state != 0) return evaluate(board, state, pitsPerPlayer);

    if (depth <= 0) return evaluate(board, 0, pitsPerPlayer);

    nodeCount++;
    vector<int> sortedPits;
    sortPits(board, sortedPits, isTopPlayer, depth, pitsPerPlayer);

    if (!isTopPlayer) {
        int maxEval = -999999;
        for (int i = 0; i < pitsPerPlayer; i++) {
            int pit = sortedPits[i];
            if (board[pit] == 0) continue;

            vector<uint8_t> nextBoard = board;
            bool extra = makeMove(nextBoard, pit, false, pitsPerPlayer);

            int eval = minimax(nextBoard, depth - 1, extra ? false : true, alpha, beta, pitsPerPlayer);
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
        for (int i = 0; i < pitsPerPlayer; i++) {
            int pit = sortedPits[i];
            if (board[pit] == 0) continue;

            vector<uint8_t> nextBoard = board;
            bool extra = makeMove(nextBoard, pit, true, pitsPerPlayer);

            int eval = minimax(nextBoard, depth - 1, extra ? true : false, alpha, beta, pitsPerPlayer);
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
    for (int i = 0; i < 64; i++) {
        killerMoves[i][0] = -1;
        killerMoves[i][1] = -1;
    }

    nodeCount = 0;

    jsize length = env->GetArrayLength(jBoard);
    int boardSize = length;
    int pitsPerPlayer = (boardSize - 2) / 2;

    jint *cBoard = env->GetIntArrayElements(jBoard, NULL);
    vector<uint8_t> startBoard(boardSize);
    for (int i = 0; i < boardSize; i++) {
        startBoard[i] = (uint8_t)cBoard[i];
    }
    env->ReleaseIntArrayElements(jBoard, cBoard, JNI_ABORT);

    int overallBestMove = -1;
    int finalBestScore = 0;
    
    for (int d = 1; d <= targetDepth; d++) {
        auto start = chrono::high_resolution_clock::now();
        int bestScore = isTopPlayer ? 999999 : -999999;
        int moveThisDepth = -1;

        int alpha = -999999;
        int beta = 999999;
        
        for (int i = 0; i < pitsPerPlayer; i++) {
            int pit = (isTopPlayer ? pitsPerPlayer + 1 : 0) + i;
            if (startBoard[pit] == 0) continue;

            vector<uint8_t> nextBoard = startBoard;
            bool extra = makeMove(nextBoard, pit, isTopPlayer, pitsPerPlayer);

            int score = minimax(nextBoard, d - 1, extra ? isTopPlayer : !isTopPlayer, alpha, beta, pitsPerPlayer);

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