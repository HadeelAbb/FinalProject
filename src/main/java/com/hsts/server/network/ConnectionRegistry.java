package com.hsts.server.network;

import ocsf.server.ConnectionToClient;
import server.controllers.AuthenticatedSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe mapping between authenticated user ids and live OCSF connections.
 * Populated on successful LOGIN, cleared on LOGOUT and client disconnect.
 */
public class ConnectionRegistry {

    private final ConcurrentHashMap<String, ConnectionToClient> userIdToConnection = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ConnectionToClient, String> connectionToUserId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ConnectionToClient, String> connectionToRole = new ConcurrentHashMap<>();

    public void register(String userId, ConnectionToClient client) {
        register(userId, null, client);
    }

    public void register(String userId, String role, ConnectionToClient client) {
        if (userId == null || userId.isBlank() || client == null) {
            return;
        }

        unregisterByConnection(client);

        ConnectionToClient previousClient = userIdToConnection.put(userId, client);
        if (previousClient != null && previousClient != client) {
            connectionToUserId.remove(previousClient);
            connectionToRole.remove(previousClient);
        }

        connectionToUserId.put(client, userId);
        if (role != null && !role.isBlank()) {
            connectionToRole.put(client, role);
        }
    }

    public void unregisterByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        ConnectionToClient client = userIdToConnection.remove(userId);
        if (client != null) {
            connectionToUserId.remove(client);
            connectionToRole.remove(client);
        }
    }

    public void unregisterByConnection(ConnectionToClient client) {
        if (client == null) {
            return;
        }

        String userId = connectionToUserId.remove(client);
        connectionToRole.remove(client);
        if (userId != null) {
            userIdToConnection.remove(userId, client);
        }
    }

    public ConnectionToClient getConnection(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return userIdToConnection.get(userId);
    }

    public String getUserId(ConnectionToClient client) {
        if (client == null) {
            return null;
        }
        return connectionToUserId.get(client);
    }

    public AuthenticatedSession getSession(ConnectionToClient client) {
        String userId = getUserId(client);
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return new AuthenticatedSession(userId, connectionToRole.get(client));
    }
}
