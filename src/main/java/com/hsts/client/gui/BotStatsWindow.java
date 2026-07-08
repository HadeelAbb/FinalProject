package com.hsts.client.gui;

import com.hsts.client.controller.BotStatsClientController;
import com.hsts.shared.model.BotUsageStats;
import com.hsts.shared.model.Teacher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class BotStatsWindow {

    @FXML private ListView<BotUsageStats> statsListView;
    @FXML private Label errorLabel;

    private BotStatsClientController controller;

    public void init(BotStatsClientController controller, Teacher teacher) {
        this.controller = controller;
        controller.setCurrentTeacher(teacher);
        controller.setView(this);
        controller.refreshStats();
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
