package com.hsts.client;

import com.hsts.client.controller.LoginClientController;
import com.hsts.client.gui.LoginWindow;
import com.hsts.client.gui.NavigationHelper;
import com.hsts.client.login.LoginManager;
import com.hsts.client.network.ConnectionState;
import com.hsts.client.network.MockServerConnection;
import com.hsts.client.network.RealServerConnection;
import com.hsts.client.network.ServerConnection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainApp extends Application {

    // Flip to true to run the GUI standalone against MockServerConnection,
    // with no live HSTSServer needed - useful if the real server crashes
    // or isn't running yet during a demo.
    private static final boolean USE_MOCK_SERVER = false;

    // TEMP: confirm with whoever runs the real HSTSServer that this matches.
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 3000;

    // Baked-in failed-login policy (Lab2/3 asked for n/t on a setup screen;
    // fixed here since a student logging into HSTS shouldn't see that screen).
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int BLOCK_DURATION_SECONDS = 30;

    // Shared LoginManager instance, same pattern as Lab2/3's MainApp.
    private static LoginManager loginManager;

    private final AtomicBoolean intentionalShutdown = new AtomicBoolean(false);
    private final AtomicBoolean disconnectAlertShown = new AtomicBoolean(false);
    private final AtomicBoolean shutdownCompleted = new AtomicBoolean(false);

    private Stage primaryStage;
    private ServerConnection serverConnection;
    private RealServerConnection realServerConnection;
    private LoginClientController loginController;

    public static LoginManager getLoginManager() {
        return loginManager;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        loginManager = new LoginManager(MAX_FAILED_ATTEMPTS, BLOCK_DURATION_SECONDS);

        if (USE_MOCK_SERVER) {
            serverConnection = new MockServerConnection();
        } else {
            RealServerConnection real = new RealServerConnection(SERVER_HOST, SERVER_PORT);
            realServerConnection = real;
            real.setConnectionStateHandler(this::onConnectionState);
            try {
                real.connect();
            } catch (IOException e) {
                showConnectionError(e);
                return;
            }
            serverConnection = real;
        }

        loginController = new LoginClientController(serverConnection);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/login.fxml"));
        Parent root = loader.load();
        LoginWindow loginWindow = loader.getController();
        loginWindow.setController(loginController);

        stage.setTitle("HSTS - Login");
        stage.setScene(new Scene(root, 360, 280));
        stage.show();
    }

    @Override
    public void stop() {
        intentionalShutdown.set(true);

        if (!shutdownCompleted.compareAndSet(false, true)) {
            return;
        }

        if (loginController != null) {
            try {
                loginController.logout();
            } catch (Exception e) {
                System.err.println("Warning: logout during shutdown failed: " + e.getMessage());
            }
        }

        if (realServerConnection != null) {
            try {
                realServerConnection.disconnect();
            } catch (Exception e) {
                System.err.println("Warning: disconnect during shutdown failed: " + e.getMessage());
            }
        }
    }

    /**
     * Receives socket lifecycle events from {@link RealServerConnection}.
     * Unexpected CLOSED/ERROR are handled on the JavaFX thread.
     */
    private void onConnectionState(ConnectionState state, String message) {
        if (state == ConnectionState.OPENED) {
            disconnectAlertShown.set(false);
            return;
        }

        if (state != ConnectionState.ERROR && state != ConnectionState.CLOSED) {
            return;
        }

        if (intentionalShutdown.get()) {
            return;
        }

        if (message != null && !message.isBlank()) {
            System.err.println("Connection lost: " + message);
        }

        Platform.runLater(() -> handleUnexpectedDisconnect(message));
    }

    private void handleUnexpectedDisconnect(String message) {
        if (intentionalShutdown.get()) {
            return;
        }

        // One alert + one UI recovery for the same disconnection
        // (covers both ERROR and a follow-up CLOSED).
        if (!disconnectAlertShown.compareAndSet(false, true)) {
            return;
        }

        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Lost");
            alert.setHeaderText("The connection to the server was lost.");
            String content = "Please make sure the server is running and log in again.";
            String detail = usefulDisconnectDetail(message);
            if (detail != null) {
                content = content + "\n\n" + detail;
            }
            alert.setContentText(content);
            alert.initOwner(primaryStage);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Warning: failed to show connection-lost alert: " + e.getMessage());
        }

        if (intentionalShutdown.get()) {
            return;
        }

        clearAuthenticatedState();
        returnToLoginAfterDisconnect();
    }

    private static String usefulDisconnectDetail(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String trimmed = message.trim();
        if (trimmed.length() > 180) {
            trimmed = trimmed.substring(0, 180) + "...";
        }
        // Avoid dumping noisy exception class prefixes when there is no useful detail.
        if ("Connection exception: null".equals(trimmed)
                || "Connection exception: unknown error".equals(trimmed)
                || "Connection closed".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private void clearAuthenticatedState() {
        if (loginController == null) {
            return;
        }
        try {
            loginController.logout();
        } catch (Exception e) {
            System.err.println("Warning: failed to clear authenticated state: " + e.getMessage());
        }
    }

    private void returnToLoginAfterDisconnect() {
        if (primaryStage == null || serverConnection == null) {
            return;
        }

        try {
            loginController = new LoginClientController(serverConnection);
            LoginWindow loginWindow = NavigationHelper.showLogin(primaryStage, loginController);
            if (loginWindow != null) {
                boolean connected = realServerConnection != null && realServerConnection.isConnected();
                if (!connected) {
                    loginWindow.showError(
                            "Connection unavailable. Restart the application after the server is running.");
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: failed to return to login after disconnect: " + e.getMessage());
        }
    }

    private void showConnectionError(IOException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Cannot reach HSTS server");
        alert.setHeaderText("Could not connect to " + SERVER_HOST + ":" + SERVER_PORT);
        alert.setContentText("Make sure the HSTS server is running, then restart this app.\n\n" + e.getMessage());
        alert.showAndWait();
    }
}
