package org.sample.nio;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * An implementation of reactor pattern responses IO events by dispatching
 * concurrent service requests synchronously to the associated request handlers.
 */
public class Reactor {

    private final Logger logger = LogManager.getLogger();

    //demultiplexer performs read and write selections.
    private final Selector sel;

    //Avoid deal lock when there is a thread sleeps on alove selector.
    private final ReentrantReadWriteLock srwLock = new ReentrantReadWriteLock();

    //Worker thread pool for handlers performs non-blocking actions
    private final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    //Selector worker thread pool
    private final ExecutorService selectorPool = Executors.newSingleThreadExecutor();

    private volatile Future<?> dispatcher = null;

    //Complete multiple handlers after a single selector wakeup
    private ArrayBlockingQueue<ChannelWrapper> completedHandlerQueue = new ArrayBlockingQueue<>(10);

    private Runnable dispatchTask = null;

    //Abstraction of IO events consumer
    private Consumer<SelectionKey> handleIOEvent = null;

    public Reactor() throws IOException {
        this.sel = Selector.open();

        /*
            Core implementation dispatches IO events.
            1. Recap all completed IO handler(s). Completed means all input of last selection has been read and processed.
            2. Select next set keys have are ready for operations (Read or/and Write)
            3. Dispatch ready keys and remove it before next selection.
         */
        dispatchTask = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                this.drainCompleteHandlerQueue();
                try {
                    /*
                        selectorGuardBarrier is handshake between new channel(s) registration and this selector.
                        Selector is awaken when there is a new channel registration then waits
                        until channel competes its registration. Otherwise, that channel may not
                        be selected due to race conditions of registration thread and this selection thread.
                     */
                    selectorGuardBarrier();
                    this.sel.select();
                    Set<SelectionKey> keys = this.sel.selectedKeys();
                    keys.forEach(handleIOEvent);
                    keys.clear();
                } catch (Exception e) {
                    logger.error("Dispatching failed. ", e);
                    this.stop();
                }
            }
        };

        handleIOEvent = (key) -> {

            ChannelWrapper handler = (ChannelWrapper) key.attachment();

            handler.prepare(); //Disable all interesting ops, otherwise, it would be fired and picked up by other workers.

            Runnable task = () -> {
                try {
                    handler.process(); //process IO events by a single worker.
                } catch (Throwable t) {
                    logger.error("Can not handler received message, force to close its connection.", t);
                    this.unRegisterChannel(handler);
                } finally {
                    addCompleteHandlerQueue(handler);
                }
            };

            this.pool.submit(task);
        };
    }

    public void start() {
        if (this.dispatcher != null) {
            return;
        }
        this.dispatcher = this.selectorPool.submit(this.dispatchTask);
    }

    public void stop() {
        this.dispatcher.cancel(true);
        try {
            this.pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("stop dispatcher is interrupted.", e);
        }
        this.dispatcher = null;
    }

    /**
     * Invoker typically a server socket acceptor registers accepted socket(s) to this reactor.
     * @param sc Accepted socket channel.
     * @param handler Implementation processes incoming data from the socket channel.
     * @return Wrapper of channel and handler.
     * @throws IOException
     */
    public ChannelWrapper registerChannel(SocketChannel sc, IHandler handler) throws IOException {
        acquireSelectorRLock();
        try {
            sc.configureBlocking(false);
            ChannelWrapper wrapper = new ChannelWrapper(sc, handler);
            SelectionKey key = sc.register(this.sel, SelectionKey.OP_READ, wrapper);
            wrapper.setKey(key);
            logger.printf(Level.INFO, "accept a new connection form %s %n", sc.getRemoteAddress());
            return wrapper;
        } finally {
            releaseSelectorRLock();
        }
    }

    /**
     * Un-register a socket channel
     * @param wrapper
     */
    public void unRegisterChannel(ChannelWrapper wrapper) {
        acquireSelectorRLock();
        try {
            SelectionKey key = wrapper.getKey();
            key.cancel();
            key.channel().close();
        } catch (IOException e) {
            logger.error("not able to close channel.", e);
        } finally {
            releaseSelectorRLock();
        }
    }

    /**
     *  Below are private method of this reactors.
     */
    private void selectorGuardBarrier() {
        srwLock.writeLock().lock();
        srwLock.writeLock().unlock();
    }

    private void acquireSelectorRLock() {
        srwLock.readLock().lock();
        this.sel.wakeup();
    }

    private void releaseSelectorRLock() {
        srwLock.readLock().unlock();
    }

    private void addCompleteHandlerQueue(ChannelWrapper handler) {
        while (!completedHandlerQueue.offer(handler)) {
            ; //completed handler is queued for next selection.
        }
        sel.wakeup();
    }

    private void drainCompleteHandlerQueue() {
        ChannelWrapper handler = null;
        while ((handler = this.completedHandlerQueue.poll()) != null) {
            if (handler.isDone()) {
                logger.printf(Level.INFO, "Close a connection %s.", handler.getRemoteAddr());
                continue;
            }
            handler.restoreOps();
        }
    }
}