package com.hsts.client.gui;

import com.hsts.client.controller.GradingClientController;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Teacher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

public class GradingWindow {

    @FXML private ListView<ExamAnswer> pendingListView;
    @FXML private Label detailsLabel;
    @FXML private Label autoScoreLabel;
    @FXML private TextField finalScoreField;
    @FXML private TextArea commentField;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private GradingClientController controller;

    public void init(GradingClientController controller, Teacher teacher) {
        this.controller = controller;
        controller.setCurrentTeacher(teacher);
        controller.setView(this);

        pendingListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showDetails(newVal));

        controller.refreshPending();
    }

    private void showDetails(ExamAnswer answer) {
        if (answer == null) {
            detailsLabel.setText("");
            autoScoreLabel.setText("");
            finalScoreField.setText("");
            return;
        }
        detailsLabel.setText("Exam " + answer.getExamId() + " - student " + answer.getStudentId());
        autoScoreLabel.setText("Automatic score: " + answer.getAutoScore());
        finalScoreField.setText(String.valueOf(answer.getAutoScore()));
        commentField.clear();
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        controller.refreshPending();
    }

    @FXML
    void handleConfirm(ActionEvent event) {
        ExamAnswer selected = pendingListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a submission first.");
            return;
        }
        Double finalScore;
        try {
            finalScore = Double.parseDouble(finalScoreField.getText());
        } catch (NumberFormatException e) {
            showError("Final score must be a number.");
            return;
        }
        controller.confirmGrade(selected.getExamAnswerId(), finalScore, commentField.getText());
    }

    public void displayPending(List<ExamAnswer> pending) {
        pendingListView.getItems().setAll(pending);
        errorLabel.setText("");
    }

    public void onGradeConfirmed(ExamAnswer answer) {
        statusLabel.setText("Grade confirmed for " + answer.getStudentId() + ": " + answer.getFinalScore());
        detailsLabel.setText("");
        autoScoreLabel.setText("");
        finalScoreField.clear();
        commentField.clear();
    }

    public void showError(String message) {
        errorLabel.setText(message);
        statusLabel.setText("");
    }
}
