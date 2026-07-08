package com.hsts.client.controller;

import com.hsts.client.gui.LoginWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.User;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.LogoutData;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;

public class LoginClientController implements ResponseHandler {

    private final ServerConnection client;
    private User currentUser;
    private LoginWindow view;
    private String lastAttemptedUsername;

    public LoginClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.LOGIN, this);
    }

    public void setView(LoginWindow view) {
        this.view = view;
    }

    public ServerConnection getClient() {
        return client;
    }

    public void login(String username, String password) {
        this.lastAttemptedUsername = username;
        client.sendToServer(Command.LOGIN, new LoginData(username, password));
    }

    /**
     * FIX: previously this only cleared local client state, never telling
     * the server. That left the username permanently marked "logged in" on
     * the server side, so logging in again as that user would always be
     * rejected until the server process itself was restarted. Now it sends
     * a real LOGOUT request so the server can release the username.
     */
    public void logout() {
        if (currentUser != null) {
            client.sendToServer(Command.LOGOUT, new LogoutData(currentUser.getId()));
        }
        currentUser = null;
    }

    @Override
    public void handleResponse(Response response) {
        handleLoginResponse(response);
    }

    public void handleLoginResponse(Response response) {
        if (response.isSuccess()) {
            currentUser = (User) response.getPayload();
            if (view != null) {
                view.onLoginSuccess(currentUser);
            }
        } else {
            if (view != null) {
                view.onLoginFailed(lastAttemptedUsername, response.getMessage());
            }
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
