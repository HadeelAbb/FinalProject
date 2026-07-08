package com.hsts.client.network;

import ocsf.client.AbstractClient;
import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;

import java.io.IOException;
import java.util.function.Consumer;

public class HSTSClient extends AbstractClient {

    private Consumer<Response> responseHandler;

    public HSTSClient(String host, int port) {
        super(host, port);
    }

    public void setResponseHandler(Consumer<Response> responseHandler) {
        this.responseHandler = responseHandler;
    }

    public void connectToServer() throws IOException {
        if (!isConnected()) {
            openConnection();
        }
    }

    public void disconnectFromServer() throws IOException {
        if (isConnected()) {
            closeConnection();
        }
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
        if (responseHandler != null) {
            responseHandler.accept(Response.success(null, null, "Connection established", null));
        }
    }

    @Override
    protected void connectionClosed() {
        if (responseHandler != null) {
            responseHandler.accept(Response.success(null, null, "Connection closed", null));
        }
    }

    @Override
    protected void connectionException(Exception exception) {
        if (responseHandler != null) {
            responseHandler.accept(
                    Response.failure(null, "Connection exception: " + exception.getMessage(), null));
        }
    }
}
