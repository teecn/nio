package org.sample.nio.buffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.function.Supplier;


public class ByteBufferQueue {

    //A zero length buffer to eliminate null check.
    private final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    private Logger logger = LogManager.getLogger();

    private IByteBufferPool bufPool = null;

    private ByteBuffer active = EMPTY_BUFFER;

    private LinkedList<ByteBuffer> fifoBufList = new LinkedList<>();

    private int bufferedBytes = 0;

    public ByteBufferQueue() {

        bufPool = new IByteBufferPool() {
            @Override
            public ByteBuffer acquire() {
                return ByteBuffer.allocate(512);
            }

            @Override
            public void release(ByteBuffer buf) {

            }
        };
    }

    public ByteBufferQueue(Supplier<IByteBufferPool> supplier) {
        this.bufPool = supplier.get();
    }

    /**
     * Copy bytes from input buffer to this queue and returns number of copied bytes.
     * @param buffer
     * @return
     */
    public int equeue(ByteBuffer buffer) {
        int nBuffered = appendToBuffers(buffer);
        this.bufferedBytes += nBuffered;
        return nBuffered;
    }

    /**
     * Return a buffer with all queued bytes.
     * @return
     */
    public ByteBuffer dequeue() {
        return this.dequeue(bufferedBytes);
    }

    /**
     * Return a buffer has up to length bytes.
     * @param length
     * @return
     */
    public ByteBuffer dequeue(int length) {

        if (this.isEmpty()) {
            return null;
        }

        int nBytesToDQ = Math.min(bufferedBytes, length);

        ByteBuffer retBuf = ByteBuffer.allocate(nBytesToDQ);

        while (nBytesToDQ > 0 && (active = this.nextBufferToRead()).hasRemaining()) {
            int ncopy = this.bufferCopy(active, retBuf);
            nBytesToDQ -= ncopy;
            bufferedBytes -= ncopy;

            if (!active.hasRemaining()) {
                this.active = this.freeBuffer(this.active);
            }
        }

        retBuf.flip();

        return retBuf;
    }

    /**
     * Search and return index of specific byte in queue, -1 means not found.
     * @param b
     * @return
     */
    public int indexOf(byte b) {
        int idx = -1;

        int nBytesBeforeFound = (idx = indexOf(active.slice(), b)) > -1 ? idx : active.remaining();
        if (idx > -1)
            return nBytesBeforeFound;

        for (ByteBuffer buf : this.fifoBufList) {
            ByteBuffer dup = buf.duplicate();
            dup.flip();

            nBytesBeforeFound += (idx = indexOf(dup, b)) > -1 ? idx : dup.remaining();

            if (idx > -1)
                return nBytesBeforeFound;
        }

        return -1;
    }

    /**
     * Any queued bytes
     * @return
     */
    public boolean isEmpty() {
        return bufferedBytes == 0;

    }


    //Search b in a buffer's all remaining bytes, return -1 if not found.
    private int indexOf(ByteBuffer buf, byte b) {
        int i, len = buf.remaining();

        for (i = 0; i < len && buf.get(i) != b; i++)
            ;

        return i == len ? -1 : i;
    }

    /*
    Below are all private methods of this queue.
     */

    //Pop buffer from stack if there is no remaining bytes left since last read
    private ByteBuffer nextBufferToRead() {
        if (this.isEmpty()) {
            return EMPTY_BUFFER;
        }

        if (active.hasRemaining()) {
            return active;
        }

        ByteBuffer temp = this.fifoBufList.removeFirst();
        temp.flip();

        return temp;
    }

    //Push a buffer to stack if there is no space in last queued buffer.
    private int appendToBuffers(ByteBuffer buf) {
        int n = 0;

        while (buf.hasRemaining()) {
            int ntopUp = topUpTolastBuffer(buf);

            if (ntopUp == 0) {
                this.fifoBufList.add(this.allocBuffer());
                continue;
            }
            n += ntopUp;
        }
        return n;
    }

    private int bufferCopy(ByteBuffer src, ByteBuffer dest) {
        int c = 0;
        while (src.hasRemaining() && dest.hasRemaining()) {
            dest.put(src.get());
            c++;
        }

        return c;
    }

    private int topUpTolastBuffer(ByteBuffer buf) {
        ByteBuffer last = this.last();
        if (last == EMPTY_BUFFER) return 0;

        int ncopy = bufferCopy(buf, last);
        this.fifoBufList.addLast(last);
        return ncopy;
    }

    private ByteBuffer allocBuffer() {
        return this.bufPool.acquire();
    }

    private ByteBuffer freeBuffer(ByteBuffer buf) {
        this.bufPool.release(buf);
        return EMPTY_BUFFER;
    }

    private ByteBuffer last() {
        return fifoBufList.isEmpty() ? EMPTY_BUFFER : fifoBufList.removeLast();
    }
}
