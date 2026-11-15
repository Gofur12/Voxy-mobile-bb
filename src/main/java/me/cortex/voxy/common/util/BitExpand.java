package me.cortex.voxy.common.util;

//Wrapper around Long/Integer.expand (bit deposit). The 1.20.1 source set replaces this class with a
//Java 17 compatible implementation, since Long.expand/Integer.expand only exist since Java 19.
public final class BitExpand {
    private BitExpand() {}

    public static long expand(long src, long mask) {
        return Long.expand(src, mask);
    }

    public static int expand(int src, int mask) {
        return Integer.expand(src, mask);
    }
}
