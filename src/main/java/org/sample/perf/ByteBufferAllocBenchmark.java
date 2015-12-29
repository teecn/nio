package org.sample.perf;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.IntStream;

/**
 * Created by leon on 12/23/15.
 */
@State(Scope.Benchmark)
public class ByteBufferAllocBenchmark {
    private ArrayBlockingQueue<ByteBuffer> pool = new ArrayBlockingQueue<ByteBuffer>(128);


    public ByteBuffer directAlloc() {
        ByteBuffer buf = ByteBuffer.allocate(512);
        return buf;
    }

    private ByteBuffer noopRelease(ByteBuffer buf) { return null;}


    public ByteBuffer pooledAlloc() {
        ByteBuffer buf = pool.poll();

        if(buf == null)
        {
            buf = this.directAlloc();
        }

        return buf;

    }

    public ByteBuffer pooledRelease(ByteBuffer buf) {
        this.pool.offer(buf);
        return null;
    }

    private  int N = 10;
    @Benchmark
    public void testDirect() {
        IntStream.range(0, N).parallel().forEach(i -> {
                noopRelease(this.directAlloc());
        });
    }

    @Benchmark
    public void testPoolAlloc() {
        IntStream.range(0, N).parallel().forEach(i -> {
            pooledRelease(this.pooledAlloc());
        });
    }

}
