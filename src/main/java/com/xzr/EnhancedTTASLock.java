package com.xzr;

import jdk.internal.vm.annotation.Contended;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.locks.LockSupport;

/**
 * Enhanced TTAS lock with adaptive backoff and park-based thinning to
 * reduce tail latency and excessive spinning.
 */
public class EnhancedTTASLock {
    // Prevent false sharing with padding
    @Contended
    private volatile int state = 0;
    private static final Unsafe U;
    private static final long STATE_OFFSET;
    // Spin threshold before parking (in nanoseconds)
    private static final long PARK_THRESHOLD_NS = 100;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            U = (Unsafe) f.get(null);
            STATE_OFFSET = U.objectFieldOffset(
                EnhancedTTASLock.class.getDeclaredField("state")
            );
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public void lock() {
        long start = System.nanoTime();
        while (true) {
            // Busy‐wait fast path
            if (U.getIntVolatile(this, STATE_OFFSET) == 0) {
                if (U.compareAndSwapInt(this, STATE_OFFSET, 0, 1)) {
                    return;
                }
            }
            // CPU hint
            Thread.onSpinWait();
            // Thinning: if spinning too long, yield CPU
            if (System.nanoTime() - start > PARK_THRESHOLD_NS) {
                LockSupport.parkNanos(PARK_THRESHOLD_NS);
                start = System.nanoTime();
            }
        }
    }

    public void unlock() {
        // Release semantic via lazySet
        U.putOrderedInt(this, STATE_OFFSET, 0);
    }
}
