package org.sample.nio.echo.server;

import org.sample.nio.IHandler;
import org.sample.nio.IOHandlerFactory;


public class EchoHandlerFactory extends IOHandlerFactory {
    @Override
    public IHandler createHandler() {
        return new EchoHandler();
    }
}
