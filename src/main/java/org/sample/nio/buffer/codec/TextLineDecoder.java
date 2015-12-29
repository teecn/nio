package org.sample.nio.buffer.codec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sample.nio.buffer.ByteBufferQueue;

import java.nio.ByteBuffer;

public class TextLineDecoder implements ByteBufferQueueDecoder<String> {
    private final Logger logger = LogManager.getLogger();

    @Override
    public String decode(ByteBufferQueue queue) {
        int position = -1;

        if( ( position = queue.indexOf((byte)'\n') ) > -1) {
            ByteBuffer buf = queue.dequeue(position + 1);
           return new String(buf.array());
        }

        return null;
    }
}
