package org.sample.nio.buffer;


public class ByteBufferPoolFactory {

    public static IByteBufferPool newByteBufferPool (int bufSize, int maxNumOfBufs) {
        return new LeakyByteBufferPool(bufSize, maxNumOfBufs);
    }
}
