package com.hsts.server.network;

import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** OCSF server wrapper. Use setRouter(...) to connect Partner 1 controllers. */
public class HSTSServer extends AbstractServer {

    private BiFunction<Request, ConnectionToClient, Response> requestHandler;
    private ServerRequestRouter router;
    private ConnectionRegistry connectionRegistry;
    private Consumer<String> userDisconnectHandler;

    public HSTSServer(int port) {
        super(port);
    }

    public void setRequestHandler(BiFunction<Request, ConnectionToClient, Response> requestHandler) {
        this.requestHandler = requestHandler;
    }

    public void setRouter(ServerRequestRouter router) {
        this.router = router;
    }

    public void setConnectionRegistry(ConnectionRegistry connectionRegistry) {
        this.connectionRegistry = connectionRegistry;
    }

    public ConnectionRegistry getConnectionRegistry() {
        return connectionRegistry;
    }

    public void setUserDisconnectHandler(Consumer<String> userDisconnectHandler) {
        this.userDisconnectHandler = userDisconnectHandler;
    }

    public void startServer() throws IOException {
        listen();
    }

    public void stopServer() throws IOException {
        close();
    }

    public void sendToUser(String userId, Response response) {
        if (userId == null || userId.isBlank() || response == null || connectionRegistry == null) {
            return;
        }

        ConnectionToClient client = connectionRegistry.getConnection(userId);
        if (client != null) {
            sendResponse(client, response);
        }
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

    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        cleanupClientConnection(client);
        System.out.println("Client disconnected: " + client);
    }

    @Override
    protected void clientException(ConnectionToClient client, Throwable exception) {
        cleanupClientConnection(client);
        String detail = exception != null ? exception.getMessage() : "unknown error";
        System.err.println("Client connection exception: " + detail);
    }

    /**
     * Idempotent session/registry cleanup used for both graceful disconnect
     * and unexpected socket failures. Safe to call more than once for the
     * same connection.
     */
    private void cleanupClientConnection(ConnectionToClient client) {
        if (client == null) {
            return;
        }

        String sessionKey = resolveSessionKey(client);

        if (sessionKey != null && !sessionKey.isBlank() && userDisconnectHandler != null) {
            try {
                userDisconnectHandler.accept(sessionKey);
            } catch (Exception exception) {
                System.err.println("Disconnect handler failed for session " + sessionKey + ": "
                        + exception.getMessage());
            }
        }

        if (connectionRegistry != null) {
            connectionRegistry.unregisterByConnection(client);
        }
    }

    private String resolveSessionKey(ConnectionToClient client) {
        if (client == null) {
            return null;
        }

        Object usernameInfo = client.getInfo("username");
        if (usernameInfo instanceof String username && !username.isBlank()) {
            return username;
        }

        String userId = null;
        if (connectionRegistry != null) {
            userId = connectionRegistry.getUserId(client);
        }

        if (userId == null || userId.isBlank()) {
            Object infoUserId = client.getInfo("userId");
            if (infoUserId instanceof String id && !id.isBlank()) {
                userId = id;
            }
        }

        return userId;
    }
}
