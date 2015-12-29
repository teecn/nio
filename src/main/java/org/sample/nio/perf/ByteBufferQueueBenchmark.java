package org.sample.nio.perf;

import  org.openjdk.jmh.annotations.*;
import org.sample.nio.buffer.ByteBufferQueue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

@State(Scope.Thread)
public class ByteBufferQueueBenchmark {

    private ByteBufferQueue queue = null;
    private int BLOCK_SZ = 128;
    private int NUM_OF_LINES = 32;
    private byte[] block = new byte[BLOCK_SZ];

    {
        Arrays.fill(block, (byte)'-');
        block[BLOCK_SZ - 1] = '\n';
    }

    @Setup
    public void init() {
        this.queue = new ByteBufferQueue();
    }


    @Benchmark
    public int enqueue() {
       return IntStream.range(0, NUM_OF_LINES).mapToObj(i -> {
            return this.queue.equeue(ByteBuffer.wrap(block));
        }).mapToInt(i->i).sum();
    }


    @TearDown
    public  void destory (){
        queue.dequeue(BLOCK_SZ * NUM_OF_LINES);
        this.queue = null;
    }

}
