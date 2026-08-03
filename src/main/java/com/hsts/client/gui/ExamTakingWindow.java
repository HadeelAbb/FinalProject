package com.hsts.client.gui;

import com.hsts.client.controller.ExamTakingClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Student;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamTakingWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ListView<Exam> availableExamsView;
    @FXML private TextField executionCodeField;
    @FXML private ListView<Question> questionsView;
    @FXML private Label instructionsLabel;
    @FXML private Label timerLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private Button startButton;
    @FXML private Button submitButton;

    private ExamTakingClientController controller;
    private Exam currentExam;
    private boolean examInProgress;
    private final Map<String, ToggleGroup> answerGroups = new HashMap<>();
    private Timeline timer;
    private int secondsRemaining;

    private Student navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;

    public void init(ExamTakingClientController controller, Student student,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = student;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentStudent(student);
        controller.setView(this);

        availableExamsView.setPlaceholder(new Label("No exams available to take right now."));
        questionsView.setCellFactory(list -> new QuestionCell());
        controller.loadAvailableExams();
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (examInProgress) {
            if (!NavigationHelper.confirm("You have an exam in progress. Going back will NOT submit it, "
                    + "and your answers so far will be lost when the timer runs out unsupervised. Continue?")) {
                return;
            }
        }
        if (timer != null) {
            timer.stop();
        }
        NavigationHelper.goToDashboard(backButton, navUser, navClient, navLoginController);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        NavigationHelper.logoutWithConfirmation(logoutButton, navClient, navLoginController);
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
        String code = executionCodeField.getText() != null
                ? executionCodeField.getText().trim().toUpperCase()
                : "";
        if (code.length() != 4) {
            showError("Enter the 4-character execution code for this exam.");
            return;
        }
        startButton.setDisable(true);
        controller.startExam(selected.getExamId(), code);
    }

    public void onExamStarted(Exam exam) {
        this.currentExam = exam;
        this.examInProgress = true;
        answerGroups.clear();
        instructionsLabel.setText(exam.getInstructionsForStudents() != null
                ? exam.getInstructionsForStudents() : "");
        questionsView.getItems().setAll(exam.getQuestions());
        submitButton.setDisable(false);
        startButton.setDisable(true);
        executionCodeField.setDisable(true);
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
        long answered = answerGroups.values().stream().filter(g -> g.getSelectedToggle() != null).count();
        long total = currentExam != null ? currentExam.getQuestions().size() : 0;
        String message = answered < total
                ? "You've answered " + answered + " of " + total + " questions. Submit anyway?"
                : "Submit your exam now? You won't be able to change your answers after this.";
        if (!NavigationHelper.confirm(message)) {
            return;
        }
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
        examInProgress = false;
        statusLabel.setText("Submitted. Auto-graded score: " + answer.getAutoScore()
                + " (pending teacher confirmation before it appears in your results).");
        questionsView.getItems().clear();
        instructionsLabel.setText("");
        timerLabel.setText("");
        startButton.setDisable(false);
        executionCodeField.setDisable(false);
        executionCodeField.clear();
        controller.loadAvailableExams();
    }

    public void showError(String message) {
        errorLabel.setText(message);
        startButton.setDisable(false);
        submitButton.setDisable(currentExam == null || !examInProgress);
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