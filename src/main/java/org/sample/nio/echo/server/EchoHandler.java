package org.sample.nio.echo.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sample.nio.IHandler;
import org.sample.nio.buffer.ByteBufferPoolFactory;
import org.sample.nio.buffer.ByteBufferQueue;
import org.sample.nio.buffer.IByteBufferPool;
import org.sample.nio.buffer.codec.TextLineDecoder;
import org.sample.nio.buffer.codec.TextLineEncoder;

import java.util.function.Supplier;


public class EchoHandler implements IHandler {

    private static final Logger logger = LogManager.getLogger();

    private ByteBufferQueue outputQ = new ByteBufferQueue(bufferBool());
    private ByteBufferQueue inputQ = new ByteBufferQueue(bufferBool());

    private TextLineDecoder decoder = new TextLineDecoder();
    private TextLineEncoder encoder = new TextLineEncoder();

    @Override
    public void handle() {
        String inMsg = null;

        while ((inMsg = decoder.decode(inputQ)) != null) {
            this.getOutputQ().equeue(encoder.encode(inMsg));
        }
    }

    @Override
    public ByteBufferQueue getOutputQ() {
        return outputQ;
    }

    @Override
    public ByteBufferQueue getInputQ() {
        return inputQ;
    }


    private static final IByteBufferPool sharedBufferPool = ByteBufferPoolFactory.newByteBufferPool(1024, 16);

    private Supplier<IByteBufferPool> bufferBool() {
        return () -> sharedBufferPool;
    }
}
