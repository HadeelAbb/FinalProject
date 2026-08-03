package com.hsts.client.gui;

import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Every screen after login needs the same two things: a way back to the
 * Dashboard, and a way to log out with a confirmation prompt. Centralized
 * here so each screen just calls one line instead of duplicating the
 * FXMLLoader/Alert boilerplate nine times.
 */
public final class NavigationHelper {

    private NavigationHelper() {
    }

    public static void goToDashboard(Node anyNodeInScene, User user, ServerConnection client,
                                      LoginClientController loginController) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationHelper.class.getResource("/com/hsts/client/gui/dashboard.fxml"));
            Parent root = loader.load();
            DashboardWindow dashboard = loader.getController();
            dashboard.init(user, client, loginController);

            Stage stage = (Stage) anyNodeInScene.getScene().getWindow();
            stage.setTitle("HSTS - Dashboard");
            stage.setScene(new Scene(root, 520, 680));
        } catch (IOException e) {
            // Nothing sensible to show the user here beyond staying put -
            // the Dashboard failing to load is a packaging problem, not a
            // recoverable user error.
        }
    }

    /**
     * Always confirms first - logging out mid-action (e.g. mid-exam) should
     * never happen by accident. Reuses the same ServerConnection (client)
     * for the fresh LoginClientController the new login screen needs -
     * never opens a second connection.
     */
    public static void logoutWithConfirmation(Node anyNodeInScene, ServerConnection client,
                                                LoginClientController loginController) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to log out? Any unsaved work on this screen will be lost.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Log out");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            loginController.logout();
            goToLogin(anyNodeInScene, client);
        }
    }

    /**
     * Replaces the given stage's scene with a fresh login screen using the
     * provided controller (and therefore the same {@link ServerConnection}).
     * Used by normal logout navigation and by connection-loss recovery in MainApp.
     */
    public static LoginWindow showLogin(Stage stage, LoginClientController loginController) {
        if (stage == null || loginController == null) {
            return null;
        }
        try {
            FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource("/com/hsts/client/gui/login.fxml"));
            Parent root = loader.load();
            LoginWindow loginWindow = loader.getController();
            loginWindow.setController(loginController);

            stage.setTitle("HSTS - Login");
            stage.setScene(new Scene(root, 360, 280));
            return loginWindow;
        } catch (IOException e) {
            // Same reasoning as above - a packaging problem, not something to show the user.
            return null;
        }
    }

    private static void goToLogin(Node anyNodeInScene, ServerConnection client) {
        Stage stage = (Stage) anyNodeInScene.getScene().getWindow();
        showLogin(stage, new LoginClientController(client));
    }

    /** Simple yes/no confirmation for a risky one-shot action (e.g. submitting an exam). */
    public static boolean confirm(String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}
