package org.example;

import org.example.skiplist.SkipList;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(value = 1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SkipListBenchmark {

    @Param({"1000", "10000", "50000", "100000"})
    private int dataSize;
    private SkipList skipList;

    @Setup(Level.Trial)
    public void setup() {
        skipList = new SkipList();
        for (int i = 0; i < dataSize; i++) {
            skipList.insert(ThreadLocalRandom.current().nextInt(0, 50_000));
        }
    }

    @Benchmark
    @Group("insert")
    @GroupThreads(8)
    public void testInsert(Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(0, 50_000);
        boolean result = skipList.insert(key);
        bh.consume(result);
    }

    @Benchmark
    @Group("search")
    @GroupThreads(8)
    public void testSearch(Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(0, 50_000);
        boolean found = skipList.search(key);
        bh.consume(found);
    }

    @Benchmark
    @Group("remove")
    @GroupThreads(8)
    public void testRemove(Blackhole bh) {
        int key = ThreadLocalRandom.current().nextInt(0, 50_000);
        boolean removed = skipList.remove(key);
        bh.consume(removed);
    }
}