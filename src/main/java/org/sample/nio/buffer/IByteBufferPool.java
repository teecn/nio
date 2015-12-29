package org.sample.nio.buffer;

import java.nio.ByteBuffer;

/**
 * Created by U0128754 on 12/23/2015.
 */
public interface IByteBufferPool {

    public ByteBuffer acquire();

    public void release(ByteBuffer buf);
}