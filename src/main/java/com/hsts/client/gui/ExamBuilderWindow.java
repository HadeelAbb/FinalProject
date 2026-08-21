package com.hsts.client.gui;

import com.hsts.client.controller.ExamBuilderClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamDraftActions;
import com.hsts.shared.model.ExamStatus;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExamBuilderWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ComboBox<Exam> existingExamSelector;
    @FXML private Button newExamButton;
    @FXML private Label editBanner;
    @FXML private ComboBox<Course> courseSelector;
    @FXML private TextField titleField;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private RadioButton manualModeRadio;
    @FXML private RadioButton autoModeRadio;
    @FXML private VBox manualModeBox;
    @FXML private VBox autoModeBox;
    @FXML private ListView<QuestionCheckItem> questionListView;
    @FXML private Label pointsTotalLabel;
    @FXML private TextField autoTopicField;
    @FXML private ComboBox<Difficulty> autoDifficultySelector;
    @FXML private Spinner<Integer> autoCountSpinner;
    @FXML private TextArea instructionsField;
    @FXML private TextArea teacherNotesField;
    @FXML private Button createButton;
    @FXML private Button saveVersionButton;
    @FXML private Button reviewDraftButton;
    @FXML private Button submitForApprovalButton;
    @FXML private Label draftSummaryLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private ExamBuilderClientController controller;
    private Teacher currentTeacher;
    private User navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;
    private Exam sourceExam;
    private boolean applyingExamSelection;

    public void init(ExamBuilderClientController controller, Teacher teacher,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.currentTeacher = teacher;
        this.navUser = teacher;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentTeacher(teacher);
        controller.setView(this);

        courseSelector.getItems().setAll(teacher.getCourses());
        courseSelector.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldC, newC) -> { if (newC != null) controller.searchQuestionsForCourse(newC.getId()); });

        durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 300, 60, 5));
        autoCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 5, 1));
        autoDifficultySelector.getItems().setAll(Difficulty.values());

        existingExamSelector.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldExam, newExam) -> {
                    if (applyingExamSelection) {
                        return;
                    }
                    if (newExam != null) {
                        loadExam(newExam);
                    }
                });

        questionListView.setPlaceholder(new Label("Select a course to see its questions."));
        questionListView.setCellFactory(list -> new QuestionPointsCell());
        refreshPointsTotal();

        ToggleGroup modeGroup = new ToggleGroup();
        manualModeRadio.setToggleGroup(modeGroup);
        autoModeRadio.setToggleGroup(modeGroup);
        manualModeRadio.setSelected(true);
        updateModeVisibility();
        modeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> updateModeVisibility());
        enterNewExamMode();
        controller.refreshMyExams();
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

    @FXML
    void handleNewExam(ActionEvent event) {
        applyingExamSelection = true;
        existingExamSelector.getSelectionModel().clearSelection();
        applyingExamSelection = false;
        enterNewExamMode();
    }

    private void enterNewExamMode() {
        sourceExam = null;
        controller.selectExistingExam(null);
        editBanner.setText("Create a new exam, or select an existing exam above to save a new version.");
        courseSelector.setDisable(false);
        autoModeRadio.setDisable(false);
        updateActionButtons(null);
        draftSummaryLabel.setText("");
        errorLabel.setText("");
        statusLabel.setText("");
    }

    private void loadExam(Exam exam) {
        sourceExam = exam;
        controller.selectExistingExam(exam);
        boolean current = exam.isLatest();
        if (!current) {
            editBanner.setText("Viewing historical Exam " + exam.getExamId() + " — Version "
                    + exam.getVersionNumber() + ". Historical versions are read-only. "
                    + "Edit the current version of this exam.");
        } else if (exam.getStatus() == ExamStatus.DRAFT) {
            editBanner.setText("Exam " + exam.getExamId() + " — Version " + exam.getVersionNumber()
                    + " (Current DRAFT). Submit this version for approval, or Save as new version "
                    + "only if you change the definition (that creates Version "
                    + (exam.getVersionNumber() + 1) + ").");
        } else if (exam.getStatus() == ExamStatus.PENDING_APPROVAL) {
            editBanner.setText("Exam " + exam.getExamId() + " — Version " + exam.getVersionNumber()
                    + " is already submitted. Saving a change will create Version "
                    + (exam.getVersionNumber() + 1) + " as a new draft.");
        } else if (exam.getStatus() == ExamStatus.REJECTED) {
            editBanner.setText("Exam " + exam.getExamId() + " — Version " + exam.getVersionNumber()
                    + " was rejected. Submit this version again, or Save as new version to keep "
                    + "the rejection history and create Version " + (exam.getVersionNumber() + 1) + ".");
        } else {
            editBanner.setText("Editing Exam " + exam.getExamId() + " — Version " + exam.getVersionNumber()
                    + ". Saving will create Version " + (exam.getVersionNumber() + 1)
                    + ". The previous version will remain unchanged.");
        }
        Course match = null;
        for (Course course : courseSelector.getItems()) {
            if (course.getId().equals(exam.getCourseId())) {
                match = course;
                break;
            }
        }
        courseSelector.setValue(match);
        courseSelector.setDisable(true);
        titleField.setText(exam.getTitle());
        durationSpinner.getValueFactory().setValue(Math.max(5, exam.getDurationMinutes()));
        instructionsField.setText(exam.getInstructionsForStudents());
        teacherNotesField.setText(exam.getInstructionsForTeacher());
        manualModeRadio.setSelected(true);
        autoModeRadio.setDisable(true);
        updateModeVisibility();
        createButton.setDisable(true);
        updateActionButtons(exam);
        draftSummaryLabel.setText(exam.toString() + " - "
                + (exam.getQuestions() != null ? exam.getQuestions().size() : 0)
                + " questions, " + exam.getDurationMinutes() + " min, status: " + exam.getStatus());
        if (match != null) {
            controller.searchQuestionsForCourse(match.getId());
        } else {
            displayQuestionBank(exam.getQuestions() != null ? exam.getQuestions() : List.of());
        }
        errorLabel.setText("");
        statusLabel.setText("");
    }

    private void updateActionButtons(Exam exam) {
        createButton.setDisable(exam != null);
        saveVersionButton.setDisable(!ExamDraftActions.canSaveNewVersion(exam));
        reviewDraftButton.setDisable(!ExamDraftActions.canReview(exam));
        submitForApprovalButton.setDisable(!ExamDraftActions.canSubmitForApproval(exam));
    }

    @FXML
    void handleSaveVersion(ActionEvent event) {
        errorLabel.setText("");
        if (sourceExam == null) {
            showError("Select an existing exam first.");
            return;
        }
        if (!sourceExam.isLatest()) {
            showError(server.controllers.ExamVersioning.HISTORICAL_NOT_EDITABLE);
            return;
        }
        if (titleField.getText() == null || titleField.getText().isBlank()) {
            showError("Enter a title.");
            return;
        }
        List<String> ids = new ArrayList<>();
        Map<String, Integer> points = new LinkedHashMap<>();
        for (QuestionCheckItem item : questionListView.getItems()) {
            if (item.isSelected()) {
                ids.add(item.getQuestion().getQuestionId());
                points.put(item.getQuestion().getQuestionId(), item.getPoints());
            }
        }
        if (ids.isEmpty()) {
            showError("Select at least one question.");
            return;
        }
        String pointsError = server.controllers.ExamQuestionPointsValidator.validate(ids, points);
        if (pointsError != null) {
            showError(pointsError);
            return;
        }
        saveVersionButton.setDisable(true);
        reviewDraftButton.setDisable(true);
        submitForApprovalButton.setDisable(true);
        statusLabel.setText("Saving new exam version...");
        controller.createVersion(sourceExam.getExamId(), titleField.getText(), instructionsField.getText(),
                teacherNotesField.getText(), ids, points, durationSpinner.getValue());
    }

    public void displayMyExams(List<Exam> exams) {
        applyingExamSelection = true;
        Exam previouslySelected = existingExamSelector.getValue();
        existingExamSelector.getItems().setAll(exams);
        if (previouslySelected != null) {
            for (Exam exam : exams) {
                if (exam.equals(previouslySelected)) {
                    existingExamSelector.getSelectionModel().select(exam);
                    applyingExamSelection = false;
                    return;
                }
            }
        }
        applyingExamSelection = false;
    }

    public void displayQuestionBank(List<Question> questions) {
        List<Question> merged = new ArrayList<>(questions);
        if (sourceExam != null && sourceExam.getQuestions() != null) {
            for (Question sourceQuestion : sourceExam.getQuestions()) {
                boolean alreadyListed = merged.stream()
                        .anyMatch(q -> q.getQuestionId().equals(sourceQuestion.getQuestionId()));
                if (!alreadyListed) {
                    merged.add(sourceQuestion);
                }
            }
        }
        List<QuestionCheckItem> items = new ArrayList<>();
        for (Question q : merged) {
            QuestionCheckItem item = new QuestionCheckItem(q);
            if (sourceExam != null && sourceExam.getQuestions() != null) {
                for (Question sourceQuestion : sourceExam.getQuestions()) {
                    if (sourceQuestion.getQuestionId().equals(q.getQuestionId())) {
                        item.selectedProperty().set(true);
                        item.pointsProperty().set(sourceQuestion.getPoints());
                        break;
                    }
                }
            }
            items.add(item);
        }
        questionListView.getItems().setAll(items);
        refreshPointsTotal();
    }

    @FXML
    void handleCreate(ActionEvent event) {
        errorLabel.setText("");
        if (sourceExam != null) {
            showError("Use Save as new version to keep the previous exam unchanged.");
            return;
        }
        Course course = courseSelector.getValue();
        if (course == null || titleField.getText() == null || titleField.getText().isBlank()) {
            showError("Choose a course and enter a title.");
            return;
        }
        createButton.setDisable(true);
        reviewDraftButton.setDisable(true);
        submitForApprovalButton.setDisable(true);
        statusLabel.setText("Creating exam...");
        if (manualModeRadio.isSelected()) {
            List<String> ids = new ArrayList<>();
            Map<String, Integer> points = new LinkedHashMap<>();
            for (QuestionCheckItem item : questionListView.getItems()) {
                if (item.isSelected()) {
                    ids.add(item.getQuestion().getQuestionId());
                    points.put(item.getQuestion().getQuestionId(), item.getPoints());
                }
            }
            if (ids.isEmpty()) {
                showError("Select at least one question.");
                return;
            }
            String pointsError = server.controllers.ExamQuestionPointsValidator.validate(ids, points);
            if (pointsError != null) {
                showError(pointsError);
                return;
            }
            controller.createManual(course.getId(), titleField.getText(), instructionsField.getText(),
                    teacherNotesField.getText(), ids, points, durationSpinner.getValue());
        } else {
            controller.createAuto(course.getId(), titleField.getText(), instructionsField.getText(),
                    teacherNotesField.getText(), autoTopicField.getText(), autoDifficultySelector.getValue(),
                    autoCountSpinner.getValue(), durationSpinner.getValue());
        }
    }

    @FXML
    void handleSubmitForApproval(ActionEvent event) {
        Exam draft = controller.getCurrentDraft();
        if (!ExamDraftActions.canSubmitForApproval(draft)) {
            showError("Select the current draft to submit for approval.");
            return;
        }
        submitForApprovalButton.setDisable(true);
        controller.submitForApproval();
    }

    @FXML
    void handleReviewDraft(ActionEvent event) {
        Exam draft = controller.getCurrentDraft();
        if (draft == null) {
            showError("Create a draft first.");
            return;
        }
        ExamDraftReviewWindow.open(reviewDraftButton.getScene().getWindow(), draft);
    }

    public void onExamCreated(Exam exam) {
        sourceExam = exam;
        controller.selectExistingExam(exam);
        updateActionButtons(exam);
        draftSummaryLabel.setText(exam.toString() + " - " + exam.getQuestions().size() + " questions, "
                + exam.getDurationMinutes() + " min, status: " + exam.getStatus());
        statusLabel.setText(exam.getVersionNumber() > 1
                ? "New version created as draft. Review it, then submit for approval."
                : "Draft created. Review it, then submit for approval.");
        errorLabel.setText("");
        if (exam.getStatus() == ExamStatus.DRAFT && exam.isLatest()) {
            editBanner.setText("Exam " + exam.getExamId() + " — Version " + exam.getVersionNumber()
                    + " (Current DRAFT). Submit this version for approval, or Save as new version "
                    + "only if you change the definition (that creates Version "
                    + (exam.getVersionNumber() + 1) + ").");
        }
        controller.refreshMyExams();
    }

    public void onSubmittedForApproval(Exam exam) {
        sourceExam = exam;
        controller.selectExistingExam(exam);
        updateActionButtons(exam);
        draftSummaryLabel.setText(exam.toString() + " - "
                + (exam.getQuestions() != null ? exam.getQuestions().size() : 0)
                + " questions, " + exam.getDurationMinutes() + " min, status: " + exam.getStatus());
        statusLabel.setText("Submitted for approval - status: " + exam.getStatus());
        controller.refreshMyExams();
    }

    /** Live push: the exam currently being viewed just got approved/rejected elsewhere. */
    public void onStatusChanged(String message) {
        statusLabel.setText(message);
    }

    public void showError(String message) {
        updateActionButtons(sourceExam);
        errorLabel.setText(message);
        statusLabel.setText("");
    }

    private void refreshPointsTotal() {
        int total = 0;
        if (questionListView != null) {
            for (QuestionCheckItem item : questionListView.getItems()) {
                if (item.isSelected()) {
                    total += item.getPoints();
                }
            }
        }
        if (pointsTotalLabel != null) {
            pointsTotalLabel.setText("Total: " + total + " / 100");
        }
    }

    /** Wraps a Question with a JavaFX BooleanProperty so ListView can render a checkbox per row. */
    public static class QuestionCheckItem {
        private final Question question;
        private final javafx.beans.property.SimpleBooleanProperty selected =
                new javafx.beans.property.SimpleBooleanProperty(false);
        private final javafx.beans.property.SimpleIntegerProperty points =
                new javafx.beans.property.SimpleIntegerProperty(0);

        public QuestionCheckItem(Question question) {
            this.question = question;
        }

        public Question getQuestion() {
            return question;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public int getPoints() {
            return points.get();
        }

        public javafx.beans.property.BooleanProperty selectedProperty() {
            return selected;
        }

        public javafx.beans.property.IntegerProperty pointsProperty() {
            return points;
        }

        @Override
        public String toString() {
            return question.toString();
        }
    }

    private class QuestionPointsCell extends javafx.scene.control.ListCell<QuestionCheckItem> {
        private final CheckBox check = new CheckBox();
        private final Label label = new Label();
        private final Spinner<Integer> spinner = new Spinner<>();
        private final HBox box = new HBox(8, check, label, spinner);
        private QuestionCheckItem bound;

        QuestionPointsCell() {
            spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0, 5));
            spinner.setPrefWidth(72);
            spinner.setEditable(true);
            spinner.setDisable(true);
            label.setWrapText(true);
            label.setMaxWidth(200);
            HBox.setHgrow(label, Priority.ALWAYS);
            box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            check.selectedProperty().addListener((obs, was, isNow) -> {
                if (bound != null) {
                    bound.selectedProperty().set(isNow);
                    spinner.setDisable(!isNow);
                    refreshPointsTotal();
                }
            });
            spinner.valueProperty().addListener((obs, oldV, newV) -> {
                if (bound != null && newV != null) {
                    bound.pointsProperty().set(newV);
                    refreshPointsTotal();
                }
            });
        }

        @Override
        protected void updateItem(QuestionCheckItem item, boolean empty) {
            super.updateItem(item, empty);
            bound = null;
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            bound = item;
            check.setSelected(item.isSelected());
            spinner.getValueFactory().setValue(item.getPoints());
            spinner.setDisable(!item.isSelected());
            label.setText(item.getQuestion().toString());
            setGraphic(box);
        }
    }
}