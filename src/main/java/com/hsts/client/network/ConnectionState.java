package com.hsts.client.network;

/**
 * Lifecycle notifications for the real socket connection.
 * Kept separate from business Command/Response routing.
 */
public enum ConnectionState {
    OPENED,
    CLOSED,
    ERROR
}
