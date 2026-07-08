package com.hsts.client.network;

import javafx.application.Platform;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wraps Partner 2's real com.hsts.client.network.HSTSClient (which extends
 * ocsf.client.AbstractClient) to actually talk to a live HSTSServer over
 * a socket.
 *
 * Two things their HSTSClient does differently from how this app was
 * originally wired, both handled here:
 *  1. It only has ONE response handler for all commands, not a per-command
 *     registry - so this class keeps its own Map<Command, ResponseHandler>
 *     and dispatches based on response.getCommand().
 *  2. Responses arrive on OCSF's background socket-reading thread, not the
 *     JavaFX Application Thread - touching any UI control from there will
 *     misbehave or throw, so every handler call is wrapped in
 *     Platform.runLater(...).
 */
public class RealServerConnection implements ServerConnection {

    private final HSTSClient realClient;
    private final Map<Command, ResponseHandler> handlers = new HashMap<>();

    public RealServerConnection(String host, int port) {
        this.realClient = new HSTSClient(host, port);
        this.realClient.setResponseHandler(this::dispatch);
    }

    /** Call once at startup, before sending anything. Throws if the server isn't reachable. */
    public void connect() throws IOException {
        realClient.connectToServer();
    }

    public void disconnect() throws IOException {
        realClient.disconnectFromServer();
    }

    @Override
    public void registerHandler(Command command, ResponseHandler handler) {
        handlers.put(command, handler);
    }

    @Override
    public void sendToServer(Command command, Object payload) {
        try {
            realClient.sendRequest(new Request(command, payload, UUID.randomUUID().toString()));
        } catch (IOException e) {
            ResponseHandler handler = handlers.get(command);
            if (handler != null) {
                Platform.runLater(() -> handler.handleResponse(
                        Response.failure(command, "Could not reach server: " + e.getMessage(), null)));
            }
        }
    }

    private void dispatch(Response response) {
        // connectionEstablished()/connectionClosed()/connectionException() in
        // their HSTSClient send Responses with command == null - not meant
        // for any ClientController, just informational.
        if (response.getCommand() == null) {
            return;
        }
        ResponseHandler handler = handlers.get(response.getCommand());
        if (handler != null) {
            Platform.runLater(() -> handler.handleResponse(response));
        }
    }
}
