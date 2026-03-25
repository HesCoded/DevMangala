#include <iostream>
#include <vector>
#include <unordered_map>
#include <fstream>
#include <chrono>

using namespace std;

uint64_t encode(uint8_t pits[12], bool isTopPlayer) {
    uint64_t key = 0;
    for (int i = 0; i < 12; ++i) {
        key |= ((uint64_t)pits[i] << (i * 5)); 
    }
    if (isTopPlayer) key |= (1ULL << 60); 
    return key;
}

void decode(uint64_t key, uint8_t pits[12], bool &isTopPlayer) {
    for (int i = 0; i < 12; ++i) {
        pits[i] = (key >> (i * 5)) & 0x1F;
    }
    isTopPlayer = (key >> 60) & 1;
}

int make_move_tb(uint8_t pits[12], int pit_index, bool isTopPlayer, bool &extra_turn) {
    uint8_t board[14];
    for (int i = 0; i < 6; i++) board[i] = pits[i];
    board[6] = 0; 
    for (int i = 0; i < 6; i++) board[i + 7] = pits[i + 6];
    board[13] = 0; 

    int actual_pit = (pit_index >= 6) ? pit_index + 1 : pit_index;
    uint8_t stones = board[actual_pit];
    board[actual_pit] = 0;

    int ownStore = isTopPlayer ? 13 : 6;
    int oppStore = isTopPlayer ? 6 : 13;
    int index = actual_pit;

    if (stones == 1) {
        index = (index + 1) % 14;
        board[index]++;
    } else {
        board[index] = 1;
        stones--;
        while (stones > 0) {
            index = (index + 1) % 14;
            if (index == oppStore) continue;
            board[index]++;
            stones--;
        }
    }

    extra_turn = (index == ownStore);

    bool isOwnSide = isTopPlayer ? (index >= 7 && index <= 12) : (index >= 0 && index <= 5);
    if (isOwnSide && board[index] == 1) {
        int oppIndex = 12 - index;
        if (board[oppIndex] > 0) {
            board[ownStore] += (board[index] + board[oppIndex]);
            board[index] = 0;
            board[oppIndex] = 0;
        }
    }

    bool isOppSide = isTopPlayer ? (index >= 0 && index <= 5) : (index >= 7 && index <= 12);
    if (isOppSide && (board[index] % 2 == 0)) {
        board[ownStore] += board[index];
        board[index] = 0;
    }

    for (int i = 0; i < 6; i++) {
        pits[i] = board[i];
        pits[i + 6] = board[i + 7];
    }

    return board[6] - board[13]; 
}

unordered_map<uint64_t, int8_t> tb;
unordered_map<uint64_t, bool> calculating; 

int8_t solve(uint64_t state) {
    if (tb.count(state)) return tb[state];
    if (calculating[state]) return 0; 
    
    calculating[state] = true;

    uint8_t pits[12];
    bool isTop;
    decode(state, pits, isTop);

    bool bottom_empty = true, top_empty = true;
    for (int i = 0; i < 6; ++i) {
        if (pits[i] > 0) {
            bottom_empty = false;
            break;
        }
    }
    for (int i = 6; i < 12; ++i) {
        if (pits[i] > 0) {
            top_empty = false;
            break;
        }
    }

    if (bottom_empty || top_empty) {
        int score = 0;
        if (bottom_empty) {
            for (int i = 6; i < 12; ++i) score += pits[i];
        } else {
            for (int i = 0; i < 6; ++i) score -= pits[i];
        }
        tb[state] = score;
        calculating[state] = false;
        return score;
    }

    int best_score = isTop ? 9999 : -9999;
    int start = isTop ? 6 : 0;

    for (int i = 0; i < 6; ++i) {
        int pit = start + i;
        if (pits[pit] == 0) continue;

        uint8_t next_pits[12];
        for (int j = 0; j < 12; ++j) next_pits[j] = pits[j];
        
        bool extra;
        int diff = make_move_tb(next_pits, pit, isTop, extra);
        uint64_t next_state = encode(next_pits, extra ? isTop : !isTop);
        
        int eval = diff + solve(next_state); 

        if (isTop) {
            if (eval < best_score) best_score = eval;
        } else {
            if (eval > best_score) best_score = eval;
        }
    }

    tb[state] = best_score;
    calculating[state] = false;
    return best_score;
}

void generate_distributions(int stones, int pit_idx, uint8_t current_pits[12], vector<uint64_t>& states, bool isTop) {
    if (pit_idx == 12) {
        if (stones == 0) states.push_back(encode(current_pits, isTop));
        return;
    }
    for (int i = 0; i <= stones; ++i) {
        current_pits[pit_idx] = i;
        generate_distributions(stones - i, pit_idx + 1, current_pits, states, isTop);
    }
}

int main() {
    auto start_time = chrono::high_resolution_clock::now();
    cout << "Tablebase generation has started (from 0 to 15 stones)..." << endl;
    
    tb.reserve(40000000);

    for (int s = 0; s <= 15; ++s) {
        vector<uint64_t> states;
        uint8_t pits[12] = {0};
        
        generate_distributions(s, 0, pits, states, false);
        generate_distributions(s, 0, pits, states, true);

        for (uint64_t state : states) {
            solve(state);
        }
        cout << s << "-stone positions solved. Total records: " << tb.size() << endl;
    }

    auto end_time = chrono::high_resolution_clock::now();
    chrono::duration<double> diff = end_time - start_time;
    cout << "Calculations has completed! Time: " << diff.count() << " seconds." << endl;

    cout << "The data is being written to mangala_15stones.bin file..." << endl;
    ofstream outfile("mangala_15stones.bin", ios::binary);
    uint64_t size = tb.size();
    outfile.write(reinterpret_cast<const char*>(&size), sizeof(size));
    
    for (auto const& [key, val] : tb) {
        outfile.write(reinterpret_cast<const char*>(&key), sizeof(key));
        outfile.write(reinterpret_cast<const char*>(&val), sizeof(val));
    }
    outfile.close();

    cout << "Completed! " << size << " positions recorded." << endl;
    return 0;
}