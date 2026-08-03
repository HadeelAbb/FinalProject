package com.hsts.client.gui;

import com.hsts.client.controller.ApprovalClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.SubjectCoordinator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class ApprovalWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ListView<Exam> pendingListView;
    @FXML private ListView<String> questionsPreview;
    @FXML private Label examDetailsLabel;
    @FXML private TextArea reasonField;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private ApprovalClientController controller;
    private ServerConnection navClient;
    private LoginClientController navLoginController;
    private SubjectCoordinator navUser;

    public void init(ApprovalClientController controller, SubjectCoordinator coordinator,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = coordinator;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentCoordinator(coordinator);
        controller.setView(this);

        pendingListView.setPlaceholder(new Label("No exams waiting for approval."));
        questionsPreview.setPlaceholder(new Label("Select an exam to preview its questions."));

        pendingListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldExam, newExam) -> showDetails(newExam));

        controller.refreshPending();
    }

    @FXML
    void handleBack(ActionEvent event) {
        NavigationHelper.goToDashboard(backButton, navUser, navClient, navLoginController);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        NavigationHelper.logoutWithConfirmation(logoutButton, navClient, navLoginController);
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
        if (!NavigationHelper.confirm("Approve \"" + selected.getTitle() + "\"? Students will be able to take it once approved.")) {
            return;
        }
        setButtonsDisabled(true);
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
        if (!NavigationHelper.confirm("Reject \"" + selected.getTitle() + "\"? The teacher will need to revise and resubmit it.")) {
            return;
        }
        setButtonsDisabled(true);
        controller.reject(selected.getExamId(), reasonField.getText());
    }

    private void setButtonsDisabled(boolean disabled) {
        approveButton.setDisable(disabled);
        rejectButton.setDisable(disabled);
    }

    public void displayPending(java.util.List<Exam> exams) {
        Exam previouslySelected = pendingListView.getSelectionModel().getSelectedItem();
        pendingListView.getItems().setAll(exams);
        errorLabel.setText("");
        setButtonsDisabled(false);

        if (previouslySelected != null) {
            // Exam now overrides equals()/hashCode() by examId, so this finds
            // the same exam even though it's a freshly-deserialized instance -
            // keeps the details/questions preview from clearing on every
            // automatic refresh (e.g. another teacher's EXAM_EVENT broadcast).
            for (Exam e : exams) {
                if (e.equals(previouslySelected)) {
                    pendingListView.getSelectionModel().select(e);
                    return;
                }
            }
        }
        // Selected exam is no longer pending (approved/rejected elsewhere) - clear the preview.
        showDetails(null);
    }

    public void onDecisionMade(Exam exam, String message) {
        setButtonsDisabled(false);
        String codeNote = exam.getExecutionCode() != null
                ? " - execution code: " + exam.getExecutionCode()
                : "";
        statusLabel.setText(message + " (" + exam.getExamId() + ")" + codeNote);
        reasonField.clear();
        controller.refreshPending();
    }

    public void showError(String message) {
        setButtonsDisabled(false);
        errorLabel.setText(message);
        statusLabel.setText("");
    }
}