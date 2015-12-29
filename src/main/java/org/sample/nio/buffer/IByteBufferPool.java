package org.sample.nio.buffer;

import java.nio.ByteBuffer;


public interface IByteBufferPool {

    public ByteBuffer acquire();

    public void release(ByteBuffer buf);
}
