package com.hsts.client.gui;

import com.hsts.client.controller.LoginClientController;
import com.hsts.client.controller.ResultsClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Student;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.util.List;

public class ResultsWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ListView<ExamAnswer> resultsListView;
    @FXML private Label scoreLabel;
    @FXML private Label commentLabel;
    @FXML private Button viewCopyButton;
    @FXML private ListView<AnswerRow> copyView;
    @FXML private Label errorLabel;

    private ResultsClientController controller;
    private Student navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;

    public void init(ResultsClientController controller, Student student,
                      ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = student;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentStudent(student);
        controller.setView(this);

        resultsListView.setPlaceholder(new Label("No confirmed results yet."));
        copyView.setPlaceholder(new Label("Select a result, then click \"View graded copy\"."));
        copyView.setCellFactory(list -> new AnswerRowCell());

        resultsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showSummary(newVal));

        controller.refreshResults();
    }

    @FXML
    void handleBack(ActionEvent event) {
        NavigationHelper.goToDashboard(backButton, navUser, navClient, navLoginController);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        NavigationHelper.logoutWithConfirmation(logoutButton, navClient, navLoginController);
    }

    private void showSummary(ExamAnswer answer) {
        copyView.getItems().clear();
        if (answer == null) {
            scoreLabel.setText("");
            commentLabel.setText("");
            return;
        }
        scoreLabel.setText("Exam " + answer.getExamId() + " - final score: " + answer.getFinalScore());
        commentLabel.setText(answer.getTeacherComment() != null
                ? "Teacher comment: " + answer.getTeacherComment() : "");
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        controller.refreshResults();
    }

    @FXML
    void handleViewCopy(ActionEvent event) {
        ExamAnswer selected = resultsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a result first.");
            return;
        }
        controller.viewCopy(selected.getExamAnswerId());
    }

    public void displayResults(List<ExamAnswer> results) {
        resultsListView.getItems().setAll(results);
        errorLabel.setText("");
    }

    public void displayCopy(Exam exam, ExamAnswer answer) {
        List<AnswerRow> rows = exam.getQuestions().stream().map(q -> {
            String mine = answer.getSelectedAnswers().get(q.getQuestionId());
            QuestionAnswer correct = q.getCorrectAnswer();
            boolean isCorrect = mine != null && correct != null && mine.equals(correct.getText());
            return new AnswerRow(q, mine, correct != null ? correct.getText() : "?", isCorrect);
        }).toList();
        copyView.getItems().setAll(rows);
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }

    private record AnswerRow(Question question, String myAnswer, String correctAnswer, boolean correct) {
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
            String myText = row.myAnswer() != null ? row.myAnswer() : "(no answer)";
            String line = row.question().getText() + "\n  your answer: " + myText
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
