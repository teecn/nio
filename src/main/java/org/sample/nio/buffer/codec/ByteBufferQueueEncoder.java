package org.sample.nio.buffer.codec;

import java.nio.ByteBuffer;

/**
 * Created by U0128754 on 12/21/2015.
 */
public interface ByteBufferQueueEncoder<T> {

    public ByteBuffer encode(T t);
}
