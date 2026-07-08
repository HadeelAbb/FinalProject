package com.hsts.server.network;

import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

import java.io.IOException;
import java.util.function.BiFunction;

/** OCSF server wrapper. Use setRouter(...) to connect Partner 1 controllers. */
public class HSTSServer extends AbstractServer {

    private BiFunction<Request, ConnectionToClient, Response> requestHandler;
    private ServerRequestRouter router;

    public HSTSServer(int port) {
        super(port);
    }

    public void setRequestHandler(BiFunction<Request, ConnectionToClient, Response> requestHandler) {
        this.requestHandler = requestHandler;
    }

    public void setRouter(ServerRequestRouter router) {
        this.router = router;
    }

    public void startServer() throws IOException {
        listen();
    }

    public void stopServer() throws IOException {
        close();
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if (!(msg instanceof Request request)) {
            sendResponse(client, Response.failure(null, "Unexpected message received from client", null));
            return;
        }

        try {
            Response response;
            if (requestHandler != null) {
                response = requestHandler.apply(request, client);
            } else if (router != null) {
                response = router.route(request);
            } else {
                response = Response.failure(request.getCommand(),
                        "No server request handler or router configured",
                        request.getRequestId());
            }

            if (response == null) {
                response = Response.failure(request.getCommand(), "Server returned no response", request.getRequestId());
            }
            sendResponse(client, response);
        } catch (Exception exception) {
            sendResponse(client, Response.failure(request.getCommand(),
                    "Server error: " + exception.getMessage(),
                    request.getRequestId()));
        }
    }

    private void sendResponse(ConnectionToClient client, Response response) {
        if (client == null || response == null) {
            return;
        }
        try {
            client.sendToClient(response);
        } catch (IOException ignored) {
            // Keep server alive even if one client fails.
        }
    }

    @Override protected void serverStarted() { System.out.println("HSTS server started on port " + getPort()); }
    @Override protected void serverStopped() { System.out.println("HSTS server stopped"); }
    @Override protected void serverClosed() { System.out.println("HSTS server closed"); }
    @Override protected void clientConnected(ConnectionToClient client) { System.out.println("Client connected: " + client); }
    @Override protected void clientDisconnected(ConnectionToClient client) { System.out.println("Client disconnected: " + client); }
}
