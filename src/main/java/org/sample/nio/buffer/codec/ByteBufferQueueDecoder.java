package org.sample.nio.buffer.codec;

import org.sample.nio.buffer.ByteBufferQueue;


public interface ByteBufferQueueDecoder<T> {

    public T decode(ByteBufferQueue queue);
}
