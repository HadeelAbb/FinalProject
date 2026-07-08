package com.hsts.shared.net.dto;

import java.io.Serializable;

/**
 * NEW: payload for Command.LOGOUT. Carries the username so the server knows
 * which entry to remove from LoginServerController's activeLoggedInUsers set.
 * Without this, a username stays permanently "logged in" on the server until
 * the server process itself is restarted, blocking that user from logging
 * in again even after closing and reopening the client.
 */
public class LogoutData implements Serializable {
    private String username;

    public LogoutData() {
    }

    public LogoutData(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
