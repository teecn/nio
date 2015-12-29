package org.sample.nio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sample.nio.buffer.ByteBufferQueue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class ChannelWrapper {

    private Logger logger = LogManager.getLogger();

    private SelectionKey key;
    private SocketChannel sc;
    private IHandler handler;
    private InetSocketAddress remoteAddr = null;

    private int intrestingOps = 0;
    private ByteBuffer inputBuf = ByteBuffer.allocate(1024);

    public ChannelWrapper(SocketChannel sc, IHandler handler) {
        this.sc = sc;
        this.handler = handler;
        try {
            this.remoteAddr = (InetSocketAddress) sc.getRemoteAddress();
        } catch (IOException e) {
            logger.error("Can not init.", e);
        }
    }

    public InetSocketAddress getRemoteAddr() {
        return remoteAddr;
    }

    public SelectionKey getKey() {
        return this.key;
    }

    public void setKey(SelectionKey key) {
        this.key = key;
    }

    public void prepare() {
        this.intrestingOps = this.key.interestOps();
        this.key.interestOps(0); //disable all ops
    }

    public void process() throws IOException {
        try {
            this.fillInput(); //Read input
            handler.handle(); //Process input
            this.drainOutput(); //Write out results
        } catch (Throwable e) {
            this.offWriteOps();
            this.offReadOps();
            throw e;
        }
    }

    public boolean isDone() {
        return this.isWriteOpsOff() && this.isReadOpsOff();
    }

    private void fillInput() throws IOException {
        int nr = sc.read(inputBuf);

        if (nr == -1) {
            logger.info("remote peer closed input stream.");
            this.offReadOps();
            this.sc.shutdownInput();
            return;
        }

        do {
            inputBuf.flip();

            int numEqueue = handler.getInputQ().equeue(inputBuf);

            if (inputBuf.hasRemaining() && numEqueue == 0) {
                logger.info("input queue is full.");
            }

            inputBuf.compact();

        } while ((nr = sc.read(inputBuf)) > 0);

        onReadOps(); //always intrests read ops
    }

    private void drainOutput() throws IOException {
        ByteBufferQueue outputQ = this.handler.getOutputQ();
        ByteBuffer output = outputQ.dequeue();

        boolean writePending = output != null;

        if (writePending) {
            do {
                while (output.hasRemaining()) {
                    sc.write(output);
                }
            } while ((output = this.handler.getOutputQ().dequeue()) != null);
        }

        if (outputQ.isEmpty() && !writePending) {
            this.offWriteOps();
        } else {
            this.onWriteOps();
        }
    }

    public void restoreOps() {
        this.key.interestOps(this.intrestingOps);
    }

    private void onReadOps() {
        intrestingOps |= SelectionKey.OP_READ;
    }

    private void offReadOps() {
        intrestingOps &= ~SelectionKey.OP_READ;
    }

    private boolean isReadOpsOff() {
        return (intrestingOps & SelectionKey.OP_READ) == 0;
    }

    private void onWriteOps() {
        intrestingOps |= SelectionKey.OP_WRITE;
    }

    private void offWriteOps() {
        intrestingOps &= ~SelectionKey.OP_WRITE;
    }

    private boolean isWriteOpsOff() {
        return (intrestingOps & SelectionKey.OP_WRITE) == 0;
    }
}
