package org.sample.nio;

import org.sample.nio.buffer.ByteBufferQueue;


public interface IHandler {

    public void handle();

    public ByteBufferQueue getOutputQ();

    public ByteBufferQueue getInputQ();

}
