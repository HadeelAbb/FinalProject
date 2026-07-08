package com.hsts.client.gui;

import com.hsts.client.controller.ApprovalClientController;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.SubjectCoordinator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class ApprovalWindow {

    @FXML private ListView<Exam> pendingListView;
    @FXML private ListView<String> questionsPreview;
    @FXML private Label examDetailsLabel;
    @FXML private TextArea reasonField;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private ApprovalClientController controller;

    public void init(ApprovalClientController controller, SubjectCoordinator coordinator) {
        this.controller = controller;
        controller.setCurrentCoordinator(coordinator);
        controller.setView(this);

        pendingListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldExam, newExam) -> showDetails(newExam));

        controller.refreshPending();
    }

    private void showDetails(Exam exam) {
        if (exam == null) {
            examDetailsLabel.setText("");
            questionsPreview.getItems().clear();
            return;
        }
        examDetailsLabel.setText(exam.getTitle() + " (" + exam.getCourseId() + ") - "
                + exam.getDurationMinutes() + " min - by " + exam.getCreatedByTeacherId());
        questionsPreview.getItems().setAll(
                exam.getQuestions().stream().map(q -> q.toString()).toList());
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        controller.refreshPending();
    }

    @FXML
    void handleApprove(ActionEvent event) {
        Exam selected = pendingListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an exam first.");
            return;
        }
        controller.approve(selected.getExamId());
    }

    @FXML
    void handleReject(ActionEvent event) {
        Exam selected = pendingListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an exam first.");
            return;
        }
        if (reasonField.getText() == null || reasonField.getText().isBlank()) {
            showError("A rejection reason is required.");
            return;
        }
        controller.reject(selected.getExamId(), reasonField.getText());
    }

    public void displayPending(java.util.List<Exam> exams) {
        pendingListView.getItems().setAll(exams);
        errorLabel.setText("");
    }

    public void onDecisionMade(Exam exam, String message) {
        statusLabel.setText(message + " (" + exam.getExamId() + ")");
        reasonField.clear();
        controller.refreshPending();
    }

    public void showError(String message) {
        errorLabel.setText(message);
        statusLabel.setText("");
    }
}
