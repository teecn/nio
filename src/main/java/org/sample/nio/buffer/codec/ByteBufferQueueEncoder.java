package org.sample.nio.buffer.codec;

import java.nio.ByteBuffer;

public interface ByteBufferQueueEncoder<T> {

    public ByteBuffer encode(T t);
}
