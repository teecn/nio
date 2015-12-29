package org.sample.nio.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This buffer pool holds up to max_bufs buffers.
 * Create a new buffer when there is no exists one available and discard released buffer(s)
 * if queue is full.
 */
class LeakyByteBufferPool implements IByteBufferPool {

    private int bufferSz = 0;
    private int max_bufs = 0;

    private ArrayBlockingQueue<ByteBuffer> pool = null;

    public LeakyByteBufferPool(int bufferSz, int max_bufs) {
        this.bufferSz = bufferSz;
        this.max_bufs = max_bufs;

        this.pool = new ArrayBlockingQueue<ByteBuffer>(max_bufs);
    }

    public ByteBuffer acquire() {

        ByteBuffer buf = this.pool.poll();

        if (buf == null) {
            buf = this.allocBuffer();
        } else {
            buf.clear();
        }

        return buf;
    }

    public void release(ByteBuffer buf) {
        if (buf == null) return;
        this.pool.offer(buf);
    }

    private ByteBuffer allocBuffer() {
        return ByteBuffer.allocate(bufferSz);
    }
}
