package com.hsts.client.network;

import ocsf.client.AbstractClient;
import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HSTSClient extends AbstractClient {

    private Consumer<Response> responseHandler;
    private BiConsumer<ConnectionState, String> connectionStateHandler;

    public HSTSClient(String host, int port) {
        super(host, port);
    }

    public void setResponseHandler(Consumer<Response> responseHandler) {
        this.responseHandler = responseHandler;
    }

    /**
     * Optional listener for socket lifecycle (open / close / error).
     * Never used for business command routing.
     */
    public void setConnectionStateHandler(BiConsumer<ConnectionState, String> connectionStateHandler) {
        this.connectionStateHandler = connectionStateHandler;
    }

    public void connectToServer() throws IOException {
        if (!isConnected()) {
            openConnection();
        }
    }

    public void disconnectFromServer() throws IOException {
        // Always close: isConnected() can briefly remain true while the
        // reader thread is exiting after the socket has already been closed.
        closeConnection();
    }

    public void sendRequest(Request request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (!isConnected()) {
            throw new IOException("Client is not connected to the server");
        }
        sendToServer(request);
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg instanceof Response) {
            if (responseHandler != null) {
                responseHandler.accept((Response) msg);
            }
        } else if (responseHandler != null) {
            responseHandler.accept(
                    Response.failure(null, "Unexpected message received from server", null));
        }
    }

    @Override
    protected void connectionEstablished() {
        notifyConnectionState(ConnectionState.OPENED, "Connection established");
    }

    @Override
    protected void connectionClosed() {
        notifyConnectionState(ConnectionState.CLOSED, "Connection closed");
    }

    @Override
    protected void connectionException(Exception exception) {
        String detail = exception != null ? exception.getMessage() : "unknown error";
        notifyConnectionState(ConnectionState.ERROR, "Connection exception: " + detail);
    }

    private void notifyConnectionState(ConnectionState state, String message) {
        if (connectionStateHandler != null) {
            connectionStateHandler.accept(state, message);
        }
    }
}
