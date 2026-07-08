package com.hsts.client.gui;

import com.hsts.client.MainApp;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.controller.QuestionClientController;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

// Same structure/naming as your Lab2/3 LoginController, plus the same
// LoginManager-based blocking/countdown behavior, adapted to react to the
// server's response (real login check) instead of a local users.txt lookup.
public class LoginWindow {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    private LoginClientController controller;

    // Tracks which username's countdown is currently being displayed on
    // screen, same purpose as in Lab2/3's LoginController.
    private String activeCountdownEmail = "";

    public void setController(LoginClientController controller) {
        this.controller = controller;
        controller.setView(this);
    }

    @FXML
    void onLoginButtonClick(ActionEvent event) {
        errorLabel.setText("");
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            showError("user or password do not match");
            return;
        }

        // If already blocked, show the live countdown and don't even contact the server.
        if (MainApp.getLoginManager().isBlocked(username)) {
            activeCountdownEmail = username;
            int remaining = MainApp.getLoginManager().getRemainingTime(username);
            errorLabel.setText("You are blocked. Please wait " + remaining + " seconds.");
            return;
        }
        // the start of client layer
        controller.login(username, password);
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }

    /**
     * Called by LoginClientController when the server rejects the login
     * (wrong username/password). Feeds Thread 1 (failed-attempt counter +
     * blocking countdown), same as Lab2/3.
     */
    public void onLoginFailed(String username, String serverMessage) {
        activeCountdownEmail = username;
        MainApp.getLoginManager().recordFailedAttempt(
                username,
                // onError - wrong credentials, not yet blocked
                () -> errorLabel.setText(serverMessage != null ? serverMessage : "user or password do not match"),
                // onCountdown - only update the screen if this is still the active username
                secondsLeft -> {
                    if (username.equals(activeCountdownEmail)) {
                        errorLabel.setText("Too many failed attempts. Please wait " + secondsLeft + " seconds.");
                    }
                },
                // onUnblocked - block is over, clear the message only if this is still the active username
                () -> {
                    if (username.equals(activeCountdownEmail)) {
                        errorLabel.setText("");
                        activeCountdownEmail = "";
                    }
                }
        );
    }

    /**
     * Called by LoginClientController when the server accepts the login.
     * Runs Thread 2 as a final check before proceeding, in case a block
     * kicked in between sending the request and getting this response.
     */
    public void onLoginSuccess(User user) {
        String username = usernameField.getText();
        MainApp.getLoginManager().checkAndLogin(
                username,
                () -> openDashboard(user),
                () -> {
                    int remaining = MainApp.getLoginManager().getRemainingTime(username);
                    errorLabel.setText("You are blocked. Please wait " + remaining + " seconds.");
                }
        );
    }

    private void openDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hsts/client/gui/dashboard.fxml"));
            Parent root = loader.load();
            DashboardWindow dashboard = loader.getController();
            dashboard.init(user, controller.getClient(), controller);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("HSTS - Dashboard");
            stage.setScene(new Scene(root, 520, 680));

            // FIX: previously nothing ever called controller.logout(), so the
            // server never released the username from its "active sessions"
            // set. Closing the window (the only way to end a session in this
            // app, since there's no explicit Logout button) now tells the
            // server to release it, so the same user can log back in later
            // without needing to restart MainServerApp.
            stage.setOnCloseRequest(event -> controller.logout());
        } catch (IOException e) {
            showError("System Error: Could not load Dashboard screen.");
        }
    }
}
