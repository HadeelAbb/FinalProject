package com.hsts.client.network;

import javafx.application.Platform;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Wraps {@link HSTSClient} for live socket communication with HSTSServer.
 *
 * Normal request/response correlation uses requestId -> one-time callback.
 * Persistent listeners (broadcasts / events) are keyed by Command.
 * Existing controllers keep using {@link #registerHandler} + {@link #sendToServer(Command, Object)};
 * each send captures the current command handler into a pending requestId entry.
 */
public class RealServerConnection implements ServerConnection {

    private final HSTSClient realClient;
    private final ConcurrentHashMap<String, ResponseHandler> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Command, CopyOnWriteArrayList<ResponseHandler>> eventListeners =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Command, ResponseHandler> commandHandlers = new ConcurrentHashMap<>();

    private BiConsumer<ConnectionState, String> connectionStateHandler;
    private volatile boolean connectionLossNotified;

    public RealServerConnection(String host, int port) {
        this.realClient = new HSTSClient(host, port);
        this.realClient.setResponseHandler(this::dispatch);
        this.realClient.setConnectionStateHandler(this::onConnectionState);
    }

    /** Call once at startup, before sending anything. Throws if the server isn't reachable. */
    public void connect() throws IOException {
        connectionLossNotified = false;
        realClient.connectToServer();
    }

    public void disconnect() throws IOException {
        realClient.disconnectFromServer();
    }

    public boolean isConnected() {
        return realClient.isConnected();
    }

    /**
     * Optional socket lifecycle listener. Invoked on the JavaFX thread when available.
     * Duplicate CLOSED/ERROR notifications for the same loss are suppressed.
     */
    public void setConnectionStateHandler(BiConsumer<ConnectionState, String> connectionStateHandler) {
        this.connectionStateHandler = connectionStateHandler;
    }

    @Override
    public void registerHandler(Command command, ResponseHandler handler) {
        if (command == null || handler == null) {
            return;
        }
        ResponseHandler previous = commandHandlers.put(command, handler);
        CopyOnWriteArrayList<ResponseHandler> listeners =
                eventListeners.computeIfAbsent(command, ignored -> new CopyOnWriteArrayList<>());
        if (previous != null) {
            listeners.remove(previous);
        }
        if (!listeners.contains(handler)) {
            listeners.add(handler);
        }
    }

    /**
     * Backward-compatible send: captures the currently registered command handler
     * as a one-time pending callback keyed by the request's requestId.
     */
    @Override
    public void sendToServer(Command command, Object payload) {
        sendToServer(command, payload, commandHandlers.get(command));
    }

    /**
     * Request/response send with an explicit one-time callback correlated by requestId.
     */
    public void sendToServer(Command command, Object payload, ResponseHandler responseHandler) {
        String requestId = UUID.randomUUID().toString();
        if (responseHandler != null) {
            pendingRequests.put(requestId, responseHandler);
        }

        try {
            realClient.sendRequest(new Request(command, payload, requestId));
        } catch (IOException e) {
            pendingRequests.remove(requestId);
            if (responseHandler != null) {
                runOnFxThread(() -> responseHandler.handleResponse(
                        Response.failure(command, "Could not reach server: " + e.getMessage(), requestId)));
            }
        }
    }

    private void dispatch(Response response) {
        if (response == null) {
            return;
        }

        // Business responses only. Connection lifecycle uses ConnectionState.
        if (response.getCommand() == null) {
            return;
        }

        String requestId = response.getRequestId();
        if (requestId != null && !requestId.isBlank()) {
            ResponseHandler pending = pendingRequests.remove(requestId);
            if (pending != null) {
                runOnFxThread(() -> pending.handleResponse(response));
                return;
            }
        }

        // Broadcasts / events (or unmatched responses): notify persistent listeners.
        List<ResponseHandler> listeners = eventListeners.get(response.getCommand());
        if (listeners != null) {
            for (ResponseHandler listener : listeners) {
                runOnFxThread(() -> listener.handleResponse(response));
            }
        }
    }

    private void onConnectionState(ConnectionState state, String message) {
        if (state == ConnectionState.OPENED) {
            connectionLossNotified = false;
        } else if (state == ConnectionState.CLOSED || state == ConnectionState.ERROR) {
            if (connectionLossNotified) {
                return;
            }
            connectionLossNotified = true;
        }

        BiConsumer<ConnectionState, String> handler = connectionStateHandler;
        if (handler == null) {
            return;
        }

        runOnFxThread(() -> handler.accept(state, message));
    }

    /**
     * UI callbacks must run on the JavaFX thread. If the toolkit is not running
     * (unit tests), invoke directly so networking tests remain usable.
     */
    private void runOnFxThread(Runnable action) {
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } catch (IllegalStateException ignored) {
            action.run();
        }
    }
}
