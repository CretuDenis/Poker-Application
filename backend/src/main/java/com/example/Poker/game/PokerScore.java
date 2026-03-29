package com.example.Poker.game;
import java.util.Arrays;

// all bytes will be set to ff when the score is for a royale flush
// straight flush = encode the largest element 5 4 3 2 1 we will store 5 in the most significant nibble  1 nibble  index    : 25
// four of a kind = store the symbol of the 4 of a kind in the next nibble and the remaining card        2 nibble  index    : 24-23
// full hosue     = store the symbol of the biggest 3 of a kind and the symbol of the pair               2 nibble  index    : 22-21
// flush          = need to store all 5 symbols of the flush                                             5 nibbles indices  : 20-19-18-17-16
// straight       = biggest of the straight                                                              1 nibble  index    : 15
// three of kind  = biggest three of a kind symbol,and the next 2 cards                                  3 nibble  index    : 14-13-12
// two pair       =                                                                                      3 nibbles indices  : 11-10-9
// one pair       = 1 nibble for the pair 3 for the other 3 cards                                        4 nibbles indices  : 8-7-6-5
// high card      = store all 5 card values                                                              5 nibbles indices  : 4-3-2-1-0

public class PokerScore {
    private byte[] data;

    public PokerScore() {
        data = new byte[13];
        reset();
    }

    // 0 = least significant nibble
    public void setNibble(int nibbleIndex,int value) {
        assert value >= 0 && value <= 15 : "Values for a nibble must be between 0 and 15";
        assert nibbleIndex >= 0 && nibbleIndex < 26 : "Score is a number with 14 bytes aka 27 nibbles";
        int byteIndex = 12 - nibbleIndex / 2;
        int nibbleOffset = nibbleIndex % 2;
        data[byteIndex] = (byte) ((data[byteIndex] & ~(0x0f << nibbleOffset * 4)) | (value << nibbleOffset * 4));
    }
 
    public void setRoyalFlush() {
        Arrays.fill(data, (byte) 0xFF);
    }

    public void reset() {
        Arrays.fill(data, (byte) 0);
    }

    public boolean greaterThan(PokerScore other) {
        return Arrays.compareUnsigned(this.data, other.data) > 0;
    }

    public boolean lessThan(PokerScore other) {
        return Arrays.compareUnsigned(this.data, other.data) < 0;
    }

    public boolean equalTo(PokerScore other) {
        return Arrays.equals(this.data, other.data);
    }

    public void print() {
        // sf  | 4oak  | fh    | flush           | st | 3oak      | 2pair     | 1pair         | highcard
        // 25  | 24-23 | 22-21 | 20-19-18-17-16  | 15 | 14-13-12  | 11-10-9   | 8-7-6-5       | 4-3-2-1-0
        int[] groupSizes = {1, 2, 2, 5, 1, 3, 3, 4, 5};

        StringBuilder sb = new StringBuilder();
        int nibbleIndex = 25;
        for (int g = 0; g < groupSizes.length; g++) {
            if (g > 0) sb.append(" | ");
            for (int n = 0; n < groupSizes[g]; n++) {
                int byteIndex = 12 - nibbleIndex / 2;
                int nibbleOffset = nibbleIndex % 2;
                int nibble = (data[byteIndex] >> (nibbleOffset * 4)) & 0x0F;
                sb.append(String.format("%X", nibble));
                nibbleIndex--;
            }
        }
        System.out.println(sb);
    }
}
