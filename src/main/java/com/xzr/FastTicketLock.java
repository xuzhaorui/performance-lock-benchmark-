package com.xzr;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

/**
 * FastTicketLock —— A high-performance spin lock
 *
 * Design points：
 1) Ticket lock: Each thread gets a unique ticket and queues it in the order of tickets to avoid CAS retry storms.
 2) Monotonous ticket number: nextTicket and nowServing are both 64-bit incremental counts, never wrapped around (overflow can be ignored in practical scenarios), and there is no ABA at all.
 3) Padding to avoid pseudo-sharing: Insert padding fields between two counts, guaranteeing they are not on the same cache line.
 4) lock() take tickets with getAndAddLong (atomic RMW); unlock() uses putOrderedLong(lazySet) to only use the release‐only store, which is the lightest memory fence.
 5) Call Thread.onSpinWait() in the spin (mapped to the CPU PAUSE) and do a light backoff if necessary.
 */
public class FastTicketLock {
    // --- Padding: Guarantees that nextTicket and nowServing will not fall on the same cache line ---
    private  long nextTicket = 0L;
    private long p1, p2, p3, p4, p5, p6,p22;
    private  long nowServing = 0L;
    private long q1, q2, q3, q4, q5, q6, q7;

    private static final Unsafe U;
    private static final long NEXT_OFFSET;
    private static final long SERVE_OFFSET;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            U = (Unsafe) f.get(null);
            NEXT_OFFSET  = U.objectFieldOffset(
                FastTicketLock.class.getDeclaredField("nextTicket")
            );
            SERVE_OFFSET = U.objectFieldOffset(
                FastTicketLock.class.getDeclaredField("nowServing")
            );
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Get Lock: Pick Up Ticket & Wait
     */
    public void lock() {
        // After getting the lock, go directly to the critical zone
        long myTicket = U.getAndAddLong(this, NEXT_OFFSET, 1);
        while (U.getLongVolatile(this, SERVE_OFFSET) != myTicket) {
            // Tiered wait strategy
            if (myTicket - U.getLongVolatile(this, SERVE_OFFSET) > 16) {
                Thread.yield(); // Give up CPU to other threads
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /**
     * Release the lock：serve number +1（release‐only store）
     */
    public void unlock() {
        // Replace putOrderedLong with stronger consistency guarantees
        U.putLongVolatile(this, SERVE_OFFSET, U.getLongVolatile(this, SERVE_OFFSET) + 1);
    }

}
