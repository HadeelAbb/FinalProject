package com.hsts.client.network;

import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * In-process stand-in for a live server - everything runs synchronously,
 * in the same call, on whatever thread calls sendToServer (normally the
 * JavaFX Application Thread itself, e.g. a button click), so no
 * Platform.runLater is needed here unlike RealServerConnection.
 */
public class MockServerConnection implements ServerConnection {

    private final Map<Command, ResponseHandler> handlers = new HashMap<>();
    private final MockServerSimulator mockServer = new MockServerSimulator();

    @Override
    public void registerHandler(Command command, ResponseHandler handler) {
        handlers.put(command, handler);
    }

    @Override
    public void sendToServer(Command command, Object payload) {
        Response response = mockServer.process(command, payload);
        ResponseHandler handler = handlers.get(response.getCommand());
        if (handler != null) {
            handler.handleResponse(response);
        }
    }
}
