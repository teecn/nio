package org.sample.nio;

import org.junit.Assert;
import org.junit.Before;
import org.sample.nio.buffer.ByteBufferPoolFactory;
import org.sample.nio.buffer.ByteBufferQueue;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class ByteBufferQueueTest {

    private int N = 10;

    private String line = String.format("%d:%s", Instant.now().getNano(), "This a line for queued for unit testing.");
    private Stream<ByteBufferQueue> queues;

    private ByteBufferQueue createQ(int bufSize) {
        return new ByteBufferQueue(() -> ByteBufferPoolFactory.newByteBufferPool(bufSize, 16));
    }

    @Before
    public void setup() {
        this.queues = IntStream.range(0, N).mapToObj(i -> 1 << (i + 1)).map(bsz -> createQ(bsz));
    }

    @org.junit.Test
    public void testEqueue() throws Exception {
        queues.forEach(
                q ->
                        Assert.assertEquals(q.equeue(ByteBuffer.wrap(line.getBytes())), line.length())
        );
    }

    @org.junit.Test
    public void testDequeue() throws Exception {
        queues.forEach(
                q ->
                {
                    q.equeue(ByteBuffer.wrap(line.getBytes()));

                    String result = new String(q.dequeue(line.length()).array());

                    Assert.assertEquals(line, result);
                }
        );
    }

    @org.junit.Test
    public void testDequeue2() throws Exception {
        queues.forEach(
                q ->
                {
                    q.equeue(ByteBuffer.wrap(line.getBytes()));

                    ByteBuffer retBuf = q.dequeue(line.length() * 10);

                    Assert.assertEquals("Buffer should have the same num of byte as its origin input byte array.", line.length(), retBuf.remaining());

                    String result = new String(retBuf.array());

                    Assert.assertEquals("Buffer should have the same content as its origin input byte array.", line, result);
                }
        );
    }

    @org.junit.Test
    public void testIndexOf() throws Exception {
        byte b2Find = '\n';
        int N = 1024, NUM_LINES = 11;
        Random ran = new Random(N);
        char[] kcs = new char[ran.nextInt(N)];

        Arrays.fill(kcs, '-');
        kcs[kcs.length - 1] = (char) b2Find;

        String line = String.valueOf(kcs);

        ByteBufferQueue bigQ = new ByteBufferQueue(() -> ByteBufferPoolFactory.newByteBufferPool(10 * N, 16));


        Stream.concat(queues, Stream.of(bigQ))
                .forEach(
                        q -> {
                            //Equeue NUM_LINES byte arrays.
                            IntStream.range(0, NUM_LINES).forEach((i) -> {
                                byte[] bytes = line.getBytes();
                                Assert.assertEquals(bytes.length, q.equeue(ByteBuffer.wrap(bytes)));
                            });

                            int p = -1, c = 0;
                            while ((p = q.indexOf(b2Find)) > -1) {
                                q.dequeue(p + 1);
                                c++;
                            }
                            Assert.assertEquals(NUM_LINES, c);
                        }
                );
    }

    @org.junit.Test
    public void testIsEmpty() throws Exception {

        queues.forEach(
                q -> {
                    Assert.assertTrue("Q should be empty", q.isEmpty());
                    q.equeue(ByteBuffer.allocate(0));

                    Assert.assertTrue("Q should be empty with a empty buffer", q.isEmpty());
                }
        );
    }
}