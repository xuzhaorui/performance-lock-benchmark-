package com.xzr;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * MCS (Mellor-Crummey and Scott) queue-based spin lock.
 * Each thread enqueues a node, spins on its own flag, and only
 * interacts with its predecessor, reducing cache line bouncing.
 */
public class MCSLock {
    private static final Unsafe U;
    private static final long TAIL_OFFSET;
    private volatile MCSNode tail;
    private static final ThreadLocal<MCSNode> CURRENT = ThreadLocal.withInitial(MCSNode::new);

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            U = (Unsafe) f.get(null);
            TAIL_OFFSET = U.objectFieldOffset(
                MCSLock.class.getDeclaredField("tail")
            );
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static class MCSNode {
        volatile boolean locked;
        volatile MCSNode next;
    }

    public void lock() {
        MCSNode node = CURRENT.get();
        node.next = null;
        node.locked = true;
        // Atomically append to queue
        MCSNode pred = (MCSNode) U.getAndSetObject(this, TAIL_OFFSET, node);
        if (pred != null) {
            pred.next = node;
            // Spin on our own flag
            while (node.locked) {
                Thread.onSpinWait();
            }
        }
        // If pred == null, lock acquired immediately
    }

    public void unlock() {
        MCSNode node = CURRENT.get();
        MCSNode succ = node.next;
        if (succ == null) {
            // No known successor, try to reset tail to null
            if (U.compareAndSwapObject(this, TAIL_OFFSET, node, null)) {
                return;
            }
            // Wait for successor to appear
            while ((succ = node.next) == null) {
                Thread.onSpinWait();
            }
        }
        // Pass the lock to successor
        succ.locked = false;
        node.next = null;  // help GC
    }
}
