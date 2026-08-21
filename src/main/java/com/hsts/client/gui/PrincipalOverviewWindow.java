package com.hsts.client.gui;

import com.hsts.client.controller.LoginClientController;
import com.hsts.client.controller.PrincipalClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.ExamStats;
import com.hsts.shared.model.GradeHistogramCalculator;
import com.hsts.shared.model.Principal;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class PrincipalOverviewWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ListView<Exam> allExamsView;
    @FXML private ListView<ExamAnswer> allResultsView;
    @FXML private Label statsLabel;
    @FXML private Label errorLabel;

    private PrincipalClientController controller;
    private Principal navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;

    public void init(PrincipalClientController controller, Principal principal,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = principal;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setView(this);

        allExamsView.setPlaceholder(new Label("No exams in the system yet."));
        allResultsView.setPlaceholder(new Label("No confirmed results yet."));

        allExamsView.getSelectionModel().selectedItemProperty().addListener((obs, oldExam, newExam) -> {
            if (newExam != null) {
                statsLabel.setText("Loading statistics...");
                controller.loadExamStats(newExam.getExamId());
            } else {
                statsLabel.setText("");
            }
        });

        controller.loadAllExams();
        controller.loadAllResults();
    }

    @FXML
    void handleBack(ActionEvent event) {
        NavigationHelper.goToDashboard(backButton, navUser, navClient, navLoginController);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        NavigationHelper.logoutWithConfirmation(logoutButton, navClient, navLoginController);
    }

    public void displayAllExams(List<Exam> exams) {
        allExamsView.getItems().setAll(exams);
        errorLabel.setText("");
    }

    public void displayAllResults(List<ExamAnswer> results) {
        allResultsView.getItems().setAll(results);
        errorLabel.setText("");
    }

    public void displayStats(ExamStats stats) {
        StringBuilder decileText = new StringBuilder();
        int[] deciles = stats.getDeciles();
        for (int i = 0; i < deciles.length; i++) {
            if (deciles[i] > 0) {
                String bucket = i < GradeHistogramCalculator.BUCKET_LABELS.length
                        ? GradeHistogramCalculator.BUCKET_LABELS[i]
                        : (i * 10) + "-" + (i * 10 + 9);
                decileText.append(bucket).append(": ")
                        .append(deciles[i]).append(" students   ");
            }
        }
        statsLabel.setText(String.format(
                "Exam %s - %d confirmed submissions - mean: %.1f - median: %.1f%s%s",
                stats.getExamId(), stats.getTotalSubmissions(), stats.getMean(), stats.getMedian(),
                decileText.length() > 0 ? "\nDistribution: " : "", decileText));
    }

    public void displayStatsUnavailable(String message) {
        statsLabel.setText(message);
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }
}