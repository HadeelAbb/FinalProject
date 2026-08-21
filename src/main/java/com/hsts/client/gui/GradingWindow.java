package com.hsts.client.gui;

import com.hsts.client.controller.GradingClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Teacher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

public class GradingWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ListView<ExamAnswer> pendingListView;
    @FXML private Label detailsLabel;
    @FXML private Label autoScoreLabel;
    @FXML private ListView<AnswerRow> answersView;
    @FXML private TextField finalScoreField;
    @FXML private TextArea commentField;
    @FXML private Button confirmButton;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private GradingClientController controller;
    private ExamAnswer selectedAnswer;
    private Teacher navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;

    public void init(GradingClientController controller, Teacher teacher,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = teacher;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentTeacher(teacher);
        controller.setView(this);

        pendingListView.setPlaceholder(new Label("Nothing waiting to be graded."));
        answersView.setPlaceholder(new Label("Select a submission to see the student's answers."));
        answersView.setCellFactory(list -> new AnswerRowCell());

        pendingListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> selectSubmission(newVal));

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

    private void selectSubmission(ExamAnswer answer) {
        selectedAnswer = answer;
        answersView.getItems().clear();
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
        controller.loadExamDetail(answer.getExamId());
    }

    public void displayExamDetail(Exam exam) {
        if (selectedAnswer == null) {
            return;
        }
        List<AnswerRow> rows = exam.getQuestions().stream().map(q -> {
            String studentAnswer = selectedAnswer.getSelectedAnswers().get(q.getQuestionId());
            QuestionAnswer correct = q.getCorrectAnswer();
            boolean isCorrect = studentAnswer != null && correct != null && studentAnswer.equals(correct.getText());
            return new AnswerRow(q, studentAnswer, correct != null ? correct.getText() : "?", isCorrect);
        }).toList();
        answersView.getItems().setAll(rows);
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
        boolean overriding = selected.getAutoScore() == null
                || Double.compare(selected.getAutoScore(), finalScore) != 0;
        if (overriding && (commentField.getText() == null || commentField.getText().isBlank())) {
            showError("A reason is required when changing the automatic grade.");
            return;
        }
        String message = overriding
                ? "You're changing the score from " + selected.getAutoScore() + " to " + finalScore
                  + " with comment: \"" + commentField.getText().trim() + "\". Continue?"
                : "Confirm the automatic score of " + finalScore + " for this student?";
        if (!NavigationHelper.confirm(message)) {
            return;
        }
        confirmButton.setDisable(true);
        controller.confirmGrade(selected.getExamAnswerId(), finalScore, commentField.getText());
    }

    public void displayPending(List<ExamAnswer> pending) {
        pendingListView.getItems().setAll(pending);
        errorLabel.setText("");
        confirmButton.setDisable(false);
    }

    public void onGradeConfirmed(ExamAnswer answer) {
        confirmButton.setDisable(false);
        String message = "Grade confirmed for " + answer.getStudentId() + ": " + answer.getFinalScore();
        statusLabel.setText(message);
        detailsLabel.setText("");
        autoScoreLabel.setText("");
        finalScoreField.clear();
        commentField.clear();
        answersView.getItems().clear();

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Grade confirmed");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(String message) {
        confirmButton.setDisable(false);
        errorLabel.setText(message);
        statusLabel.setText("");
    }

    /** One row: the question, the student's answer, the correct answer, and whether they matched. */
    private record AnswerRow(Question question, String studentAnswer, String correctAnswer, boolean correct) {
    }

    private static class AnswerRowCell extends ListCell<AnswerRow> {
        @Override
        protected void updateItem(AnswerRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                return;
            }
            String studentText = row.studentAnswer() != null ? row.studentAnswer() : "(no answer)";
            String line = row.question().getText() + "\n  student answered: " + studentText
                    + (row.correct() ? "  \u2713 correct" : "  \u2717 incorrect - correct answer: " + row.correctAnswer());
            Label label = new Label(line);
            label.setWrapText(true);
            VBox box = new VBox(4, label);
            javafx.scene.image.ImageView illustration = QuestionIllustrationView.preview(row.question(), 320, 160);
            if (illustration.getImage() != null) {
                box.getChildren().add(illustration);
            }
            setText(null);
            setGraphic(box);
            setStyle(row.correct() ? "-fx-text-fill: #1a7a1a;" : "-fx-text-fill: #b3261e;");
        }
    }
}