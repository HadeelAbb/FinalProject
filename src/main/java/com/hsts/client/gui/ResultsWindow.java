package com.hsts.client.gui;

import com.hsts.client.controller.ResultsClientController;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Student;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class ResultsWindow {

    @FXML private ListView<ExamAnswer> resultsListView;
    @FXML private Label scoreLabel;
    @FXML private Label commentLabel;
    @FXML private ListView<String> copyView;
    @FXML private Label errorLabel;

    private ResultsClientController controller;

    public void init(ResultsClientController controller, Student student) {
        this.controller = controller;
        controller.setCurrentStudent(student);
        controller.setView(this);

        resultsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showSummary(newVal));

        controller.refreshResults();
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
        List<String> lines = exam.getQuestions().stream().map(q -> {
            String mine = answer.getSelectedAnswers().get(q.getQuestionId());
            QuestionAnswer correct = q.getCorrectAnswer();
            String correctText = correct != null ? correct.getText() : "?";
            String mark = mine != null && correct != null && mine.equals(correct.getText()) ? "correct" : "incorrect";
            return q.getText() + "\n  your answer: " + mine + " (" + mark + ")"
                    + (mark.equals("incorrect") ? " - correct answer: " + correctText : "");
        }).toList();
        copyView.getItems().setAll(lines);
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }
}
