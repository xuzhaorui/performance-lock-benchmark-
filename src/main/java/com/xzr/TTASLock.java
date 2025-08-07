package com.xzr;

import jdk.internal.vm.annotation.Contended;
import sun.misc.Unsafe;

import java.lang.reflect.Field;


public class TTASLock {
    // Avoid pseudo-sharing with other fields
    @Contended
    private volatile int state = 0;
    private static final Unsafe U;
    private static final long STATE_OFFSET;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            U = (Unsafe) f.get(null);
            STATE_OFFSET = U.objectFieldOffset(
                TTASLock.class.getDeclaredField("state"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void lock() {
        int backoff = 1;
        while (true) {
            // Busy reading first to reduce CAS storms
            while (U.getIntVolatile(this, STATE_OFFSET) != 0) {
                Thread.onSpinWait();
            }
            // Really try to take the lock
            if (U.compareAndSwapInt(this, STATE_OFFSET, 0, 1)) {
                return;
            }
            // Retreat: Spin a few times and then give way
            for (int i = 0; i < backoff; i++) Thread.onSpinWait();
            if (backoff < 1<<10) backoff <<= 1;
        }
    }

    public void unlock() {
        // lazySet Just do release
        U.putOrderedInt(this, STATE_OFFSET, 0);
    }
}
