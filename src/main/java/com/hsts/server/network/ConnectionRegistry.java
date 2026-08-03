package com.hsts.server.network;

import ocsf.server.ConnectionToClient;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe mapping between authenticated user ids and live OCSF connections.
 * Populated on successful LOGIN, cleared on LOGOUT and client disconnect.
 */
public class ConnectionRegistry {

    private final ConcurrentHashMap<String, ConnectionToClient> userIdToConnection = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ConnectionToClient, String> connectionToUserId = new ConcurrentHashMap<>();

    public void register(String userId, ConnectionToClient client) {
        if (userId == null || userId.isBlank() || client == null) {
            return;
        }

        unregisterByConnection(client);

        ConnectionToClient previousClient = userIdToConnection.put(userId, client);
        if (previousClient != null && previousClient != client) {
            connectionToUserId.remove(previousClient);
        }

        connectionToUserId.put(client, userId);
    }

    public void unregisterByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        ConnectionToClient client = userIdToConnection.remove(userId);
        if (client != null) {
            connectionToUserId.remove(client);
        }
    }

    public void unregisterByConnection(ConnectionToClient client) {
        if (client == null) {
            return;
        }

        String userId = connectionToUserId.remove(client);
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
}
