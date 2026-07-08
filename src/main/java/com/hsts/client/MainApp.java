package com.hsts.client;

import com.hsts.client.controller.LoginClientController;
import com.hsts.client.gui.LoginWindow;
import com.hsts.client.login.LoginManager;
import com.hsts.client.network.MockServerConnection;
import com.hsts.client.network.RealServerConnection;
import com.hsts.client.network.ServerConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    // Flip to true to run the GUI standalone against MockServerConnection,
    // with no live HSTSServer needed - useful if the real server crashes
    // or isn't running yet during a demo.
    private static final boolean USE_MOCK_SERVER = true;

    // TEMP: confirm with whoever runs the real HSTSServer that this matches.
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 3000;

    // Baked-in failed-login policy (Lab2/3 asked for n/t on a setup screen;
    // fixed here since a student logging into HSTS shouldn't see that screen).
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int BLOCK_DURATION_SECONDS = 30;

    // Shared LoginManager instance, same pattern as Lab2/3's MainApp.
    private static LoginManager loginManager;

    public static LoginManager getLoginManager() {
        return loginManager;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        loginManager = new LoginManager(MAX_FAILED_ATTEMPTS, BLOCK_DURATION_SECONDS);

        ServerConnection serverConnection;
        if (USE_MOCK_SERVER) {
            serverConnection = new MockServerConnection();
        } else {
            RealServerConnection real = new RealServerConnection(SERVER_HOST, SERVER_PORT);
            try {
                real.connect();
            } catch (IOException e) {
                showConnectionError(e);
                return;
            }
            serverConnection = real;
        }

        LoginClientController loginController = new LoginClientController(serverConnection);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/login.fxml"));
        Parent root = loader.load();
        LoginWindow loginWindow = loader.getController();
        loginWindow.setController(loginController);

        stage.setTitle("HSTS - Login");
        stage.setScene(new Scene(root, 360, 280));
        stage.show();
    }

    private void showConnectionError(IOException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Cannot reach HSTS server");
        alert.setHeaderText("Could not connect to " + SERVER_HOST + ":" + SERVER_PORT);
        alert.setContentText("Make sure the HSTS server is running, then restart this app.\n\n" + e.getMessage());
        alert.showAndWait();
    }
}
