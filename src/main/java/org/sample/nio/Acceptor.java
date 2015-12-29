package org.sample.nio;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sample.nio.echo.server.EchoHandlerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class Acceptor {

    private static Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            printUsage();
            return;
        }

        ServerSocketChannel svr = ServerSocketChannel.open();
        svr.bind(new InetSocketAddress(Integer.parseInt(args[0])));


        Reactor reactor = new Reactor();
        reactor.start();

        logger.printf(Level.INFO, "reactor is running.");

        IOHandlerFactory ioHandlerFactory = new EchoHandlerFactory();

        SocketChannel sc = null;
        while ((sc = svr.accept()) != null) {
            reactor.registerChannel(sc, ioHandlerFactory.createHandler());
        }
    }

    private static void printUsage() {
        System.out.printf("Usage " + Acceptor.class.getSimpleName() + " <port>");
    }
}
