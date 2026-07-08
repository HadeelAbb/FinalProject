package com.hsts.client.gui;

import com.hsts.client.controller.ExamTakingClientController;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Student;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamTakingWindow {

    @FXML private ListView<Exam> availableExamsView;
    @FXML private ListView<Question> questionsView;
    @FXML private Label instructionsLabel;
    @FXML private Label timerLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.control.Button startButton;
    @FXML private javafx.scene.control.Button submitButton;

    private ExamTakingClientController controller;
    private Exam currentExam;
    private final Map<String, ToggleGroup> answerGroups = new HashMap<>();
    private Timeline timer;
    private int secondsRemaining;

    public void init(ExamTakingClientController controller, Student student) {
        this.controller = controller;
        controller.setCurrentStudent(student);
        controller.setView(this);

        questionsView.setCellFactory(list -> new QuestionCell());
        controller.loadAvailableExams();
    }

    public void displayAvailableExams(List<Exam> exams) {
        availableExamsView.getItems().setAll(exams);
        errorLabel.setText("");
    }

    @FXML
    void handleStart(ActionEvent event) {
        Exam selected = availableExamsView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an exam first.");
            return;
        }
        controller.startExam(selected.getExamId());
    }

    public void onExamStarted(Exam exam) {
        this.currentExam = exam;
        answerGroups.clear();
        instructionsLabel.setText(exam.getInstructionsForStudents() != null
                ? exam.getInstructionsForStudents() : "");
        questionsView.getItems().setAll(exam.getQuestions());
        submitButton.setDisable(false);
        startButton.setDisable(true);
        statusLabel.setText("Exam started - answer all questions before time runs out.");
        errorLabel.setText("");
        startTimer(exam.getDurationMinutes() * 60);
    }

    private void startTimer(int totalSeconds) {
        secondsRemaining = totalSeconds;
        updateTimerLabel();
        if (timer != null) {
            timer.stop();
        }
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsRemaining--;
            updateTimerLabel();
            if (secondsRemaining <= 0) {
                timer.stop();
                doSubmit(true);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void updateTimerLabel() {
        int m = Math.max(secondsRemaining, 0) / 60;
        int s = Math.max(secondsRemaining, 0) % 60;
        timerLabel.setText(String.format("Time remaining: %02d:%02d", m, s));
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        doSubmit(false);
    }

    private void doSubmit(boolean autoSubmitted) {
        if (timer != null) {
            timer.stop();
        }
        Map<String, String> answers = new HashMap<>();
        for (Map.Entry<String, ToggleGroup> entry : answerGroups.entrySet()) {
            Toggle selected = entry.getValue().getSelectedToggle();
            if (selected instanceof RadioButton rb) {
                answers.put(entry.getKey(), rb.getText());
            }
        }
        submitButton.setDisable(true);
        controller.submitExam(answers, autoSubmitted);
    }

    public void onExamSubmitted(ExamAnswer answer) {
        statusLabel.setText("Submitted. Auto-graded score: " + answer.getAutoScore()
                + " (pending teacher confirmation before it appears in your results).");
        questionsView.getItems().clear();
        instructionsLabel.setText("");
        timerLabel.setText("");
        startButton.setDisable(false);
        controller.loadAvailableExams();
    }

    public void showError(String message) {
        errorLabel.setText(message);
        submitButton.setDisable(currentExam == null);
    }

    /** Renders one question with a radio-button choice per answer. */
    private class QuestionCell extends ListCell<Question> {
        @Override
        protected void updateItem(Question question, boolean empty) {
            super.updateItem(question, empty);
            if (empty || question == null) {
                setGraphic(null);
                return;
            }
            VBox box = new VBox(4);
            box.setStyle("-fx-padding: 8 0 8 0;");
            Label text = new Label(question.getText());
            text.setWrapText(true);
            text.setStyle("-fx-font-weight: bold;");
            box.getChildren().add(text);

            ToggleGroup group = answerGroups.computeIfAbsent(question.getQuestionId(), id -> new ToggleGroup());
            for (QuestionAnswer answer : question.getAnswers()) {
                RadioButton rb = new RadioButton(answer.getText());
                rb.setToggleGroup(group);
                box.getChildren().add(rb);
            }
            setGraphic(box);
        }
    }
}
