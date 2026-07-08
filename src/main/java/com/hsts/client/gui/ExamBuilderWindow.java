package com.hsts.client.gui;

import com.hsts.client.controller.ExamBuilderClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ExamBuilderWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ComboBox<Course> courseSelector;
    @FXML private TextField titleField;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private RadioButton manualModeRadio;
    @FXML private RadioButton autoModeRadio;
    @FXML private VBox manualModeBox;
    @FXML private VBox autoModeBox;
    @FXML private ListView<QuestionCheckItem> questionListView;
    @FXML private TextField autoTopicField;
    @FXML private ComboBox<Difficulty> autoDifficultySelector;
    @FXML private Spinner<Integer> autoCountSpinner;
    @FXML private TextArea instructionsField;
    @FXML private Button createButton;
    @FXML private Label draftSummaryLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private ExamBuilderClientController controller;
    private Teacher currentTeacher;
    private User navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;

    public void init(ExamBuilderClientController controller, Teacher teacher,
                      ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.currentTeacher = teacher;
        this.navUser = teacher;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setView(this);

        courseSelector.getItems().setAll(teacher.getCourses());
        courseSelector.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldC, newC) -> { if (newC != null) controller.searchQuestionsForCourse(newC.getId()); });

        durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 300, 60, 5));
        autoCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 5, 1));
        autoDifficultySelector.getItems().setAll(Difficulty.values());

        questionListView.setPlaceholder(new Label("Select a course to see its questions."));

        ToggleGroup modeGroup = new ToggleGroup();
        manualModeRadio.setToggleGroup(modeGroup);
        autoModeRadio.setToggleGroup(modeGroup);
        manualModeRadio.setSelected(true);
        updateModeVisibility();
        modeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> updateModeVisibility());
    }

    private void updateModeVisibility() {
        boolean manual = manualModeRadio.isSelected();
        manualModeBox.setVisible(manual);
        manualModeBox.setManaged(manual);
        autoModeBox.setVisible(!manual);
        autoModeBox.setManaged(!manual);
    }

    @FXML
    void handleBack(ActionEvent event) {
        NavigationHelper.goToDashboard(backButton, navUser, navClient, navLoginController);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        NavigationHelper.logoutWithConfirmation(logoutButton, navClient, navLoginController);
    }

    public void displayQuestionBank(List<Question> questions) {
        List<QuestionCheckItem> items = new ArrayList<>();
        for (Question q : questions) {
            items.add(new QuestionCheckItem(q));
        }
        questionListView.getItems().setAll(items);
        questionListView.setCellFactory(list -> new javafx.scene.control.cell.CheckBoxListCell<>(
                QuestionCheckItem::selectedProperty));
    }

    @FXML
    void handleCreate(ActionEvent event) {
        errorLabel.setText("");
        Course course = courseSelector.getValue();
        if (course == null || titleField.getText() == null || titleField.getText().isBlank()) {
            showError("Choose a course and enter a title.");
            return;
        }
        createButton.setDisable(true);
        statusLabel.setText("Creating exam...");
        if (manualModeRadio.isSelected()) {
            List<String> ids = new ArrayList<>();
            for (QuestionCheckItem item : questionListView.getItems()) {
                if (item.isSelected()) {
                    ids.add(item.getQuestion().getQuestionId());
                }
            }
            if (ids.isEmpty()) {
                showError("Select at least one question.");
                return;
            }
            controller.createManual(course.getId(), titleField.getText(), instructionsField.getText(),
                    ids, durationSpinner.getValue());
        } else {
            controller.createAuto(course.getId(), titleField.getText(), instructionsField.getText(),
                    autoTopicField.getText(), autoDifficultySelector.getValue(),
                    autoCountSpinner.getValue(), durationSpinner.getValue());
        }
    }

    @FXML
    void handleSubmitForApproval(ActionEvent event) {
        controller.submitForApproval();
    }

    public void onExamCreated(Exam exam) {
        createButton.setDisable(false);
        draftSummaryLabel.setText(exam.getTitle() + " - " + exam.getQuestions().size() + " questions, "
                + exam.getDurationMinutes() + " min, status: " + exam.getStatus());
        statusLabel.setText("Draft created. Review it, then submit for approval.");
        errorLabel.setText("");
    }

    public void onSubmittedForApproval(Exam exam) {
        statusLabel.setText("Submitted for approval - status: " + exam.getStatus());
    }

    public void showError(String message) {
        createButton.setDisable(false);
        errorLabel.setText(message);
        statusLabel.setText("");
    }

    /** Wraps a Question with a JavaFX BooleanProperty so ListView can render a checkbox per row. */
    public static class QuestionCheckItem {
        private final Question question;
        private final javafx.beans.property.SimpleBooleanProperty selected =
                new javafx.beans.property.SimpleBooleanProperty(false);

        public QuestionCheckItem(Question question) {
            this.question = question;
        }

        public Question getQuestion() {
            return question;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public javafx.beans.property.BooleanProperty selectedProperty() {
            return selected;
        }

        @Override
        public String toString() {
            return question.toString();
        }
    }
}
