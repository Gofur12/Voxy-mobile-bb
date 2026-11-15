package me.cortex.voxy.common.util;

//Java 17 compatible replacement for Long/Integer.expand (bit deposit, PDEP semantics).
//The main source set delegates to the JDK methods, which do not exist before Java 19.
public final class BitExpand {
    private BitExpand() {}

    public static long expand(long src, long mask) {
        long result = 0;
        long bit = 1;
        while (mask != 0) {
            long lowest = mask & -mask;
            if ((src & bit) != 0) {
                result |= lowest;
            }
            mask ^= lowest;
            bit <<= 1;
        }
        return result;
    }

    public static int expand(int src, int mask) {
        int result = 0;
        int bit = 1;
        while (mask != 0) {
            int lowest = mask & -mask;
            if ((src & bit) != 0) {
                result |= lowest;
            }
            mask ^= lowest;
            bit <<= 1;
        }
        return result;
    }
}
