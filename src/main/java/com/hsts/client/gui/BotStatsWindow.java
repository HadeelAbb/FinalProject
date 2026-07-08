package com.hsts.client.gui;

import com.hsts.client.controller.BotStatsClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.BotUsageStats;
import com.hsts.shared.model.Teacher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class BotStatsWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ListView<BotUsageStats> statsListView;
    @FXML private Label errorLabel;

    private BotStatsClientController controller;
    private Teacher navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;

    public void init(BotStatsClientController controller, Teacher teacher,
                      ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = teacher;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentTeacher(teacher);
        controller.setView(this);
        statsListView.setPlaceholder(new Label("No bot activity yet for your courses."));
        controller.refreshStats();
    }

    @FXML
    void handleBack(ActionEvent event) {
        NavigationHelper.goToDashboard(backButton, navUser, navClient, navLoginController);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        NavigationHelper.logoutWithConfirmation(logoutButton, navClient, navLoginController);
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        controller.refreshStats();
    }

    public void displayStats(List<BotUsageStats> stats) {
        statsListView.getItems().setAll(stats);
        errorLabel.setText("");
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }
}
