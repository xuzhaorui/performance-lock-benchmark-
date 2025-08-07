package com.xzr;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 3)
@State(Scope.Group)
@Fork(3)
public class LockBenchmarkWithWorkload {

    // 锁实例
    private final FastTicketLock spinLock = new FastTicketLock();
    private final EnhancedTTASLock enhancedTTASLock = new EnhancedTTASLock();
    private final MCSLock mcsLock = new MCSLock();
    private final Object syncLock = new Object();
    private final TTASLock ttasLock = new TTASLock();
    private final Lock nonFairSync = new ReentrantLock(false);
    private final Lock fairSync = new ReentrantLock();

    // 临界区时间控制参数 (单位：ns)
    @Param({"50", "100", "500", "1000"})
    private int criticalDurationNs;

    // 模拟工作负载
    private long blackhole;

    @Benchmark
    @Group("spinLock")
    @GroupThreads(4)
    public void spinLockWork() {
        spinLock.lock();
        try {
            Blackhole.consumeCPU(criticalDurationNs); // 精确控制临界区时间
        } finally {
            spinLock.unlock();
        }
    }

    @Benchmark
    @Group("ttasLockWork")
    @GroupThreads(4)
    public void ttasLockWork() {
        ttasLock.lock();
        try {
            Blackhole.consumeCPU(criticalDurationNs); // 精确控制临界区时间
        } finally {
            ttasLock.unlock();
        }
    }
    @Benchmark
    @Group("nonFairSync")
    @GroupThreads(4)
    public void nonFairSyncWork() {
        nonFairSync.lock();
        try {
            Blackhole.consumeCPU(criticalDurationNs); // 精确控制临界区时间
        } finally {
            nonFairSync.unlock();
        }
    }
    @Benchmark
    @Group("fairSync")
    @GroupThreads(4)
    public void fairSyncWork() {
        fairSync.lock();
        try {
            Blackhole.consumeCPU(criticalDurationNs); // 精确控制临界区时间
        } finally {
            fairSync.unlock();
        }
    }
    @Benchmark
    @Group("synchronized")
    @GroupThreads(4)
    public void syncWork() {
        synchronized (syncLock) {
            Blackhole.consumeCPU(criticalDurationNs);
        }
    }
    @Benchmark
    @Group("enhancedTTAS")
    @GroupThreads(4)
    public void enhancedTTASLock() {
        enhancedTTASLock.lock();
        try {
            Blackhole.consumeCPU(criticalDurationNs); // 精确控制临界区时间
        } finally {
            enhancedTTASLock.unlock();
        }
    }

    @Benchmark
    @Group("mcs")
    @GroupThreads(4)
    public void mcsLock() {
        mcsLock.lock();
        try {
            Blackhole.consumeCPU(criticalDurationNs); // 精确控制临界区时间
        } finally {
            mcsLock.unlock();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LockBenchmarkWithWorkload.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
