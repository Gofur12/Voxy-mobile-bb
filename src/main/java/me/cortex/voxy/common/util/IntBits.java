package me.cortex.voxy.common.util;

/** Software versions of JDK 19+ Integer.compress/expand so the code builds with --release 17. */
public final class IntBits {
    private IntBits() {}

    /** Gathers the bits of i selected by mask into the low bits of the result (PEXT). */
    public static int compress(int i, int mask) {
        int result = 0;
        for (int bit = 1; mask != 0; bit <<= 1) {
            int lowest = mask & -mask;
            if ((i & lowest) != 0) result |= bit;
            mask &= mask - 1;
        }
        return result;
    }

    /** Deposits the low bits of i at the set bits of mask (PDEP). */
    public static int expand(int i, int mask) {
        int result = 0;
        for (int bit = 1; mask != 0; bit <<= 1) {
            int lowest = mask & -mask;
            if ((i & bit) != 0) result |= lowest;
            mask &= mask - 1;
        }
        return result;
    }
}
