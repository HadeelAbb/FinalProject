package com.hsts.client.gui;

import com.hsts.client.controller.QuestionClientController;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

import java.util.List;

public class QuestionManagementWindow {

    @FXML
    private Label loggedInLabel;

    @FXML
    private TextField searchTopicField;
    @FXML
    private ComboBox<Difficulty> searchDifficultySelector;
    @FXML
    private ComboBox<Course> searchCourseSelector;
    @FXML
    private Button searchButton;
    @FXML
    private Button newButton;

    @FXML
    private ListView<Question> questionListView;

    @FXML
    private TextArea questionTextField;
    @FXML
    private TextArea instructionsField;

    @FXML
    private RadioButton answer1Correct;
    @FXML
    private RadioButton answer2Correct;
    @FXML
    private RadioButton answer3Correct;
    @FXML
    private RadioButton answer4Correct;
    @FXML
    private TextField answer1Field;
    @FXML
    private TextField answer2Field;
    @FXML
    private TextField answer3Field;
    @FXML
    private TextField answer4Field;

    @FXML
    private ComboBox<Difficulty> difficultySelector;
    @FXML
    private TextField topicField;
    @FXML
    private ComboBox<Course> courseSelector;
    @FXML
    private TextField imagePathField;

    @FXML
    private Button saveButton;
    @FXML
    private Button deleteButton;

    @FXML
    private Label statusLabel;
    @FXML
    private Label errorLabel;

    private QuestionClientController controller;
    private Question selectedQuestion;
    private final ToggleGroup correctAnswerGroup = new ToggleGroup();

    // Used right after a save/delete to find the affected question in the
    // refreshed list and re-select it, and to stop the selection listener
    // from wiping the "saved/deleted successfully" message in the process.
    private String pendingSelectQuestionId;
    private boolean suppressMessageClear;

    @FXML
    private void initialize() {
        difficultySelector.getItems().addAll(Difficulty.values());
        searchDifficultySelector.getItems().addAll(Difficulty.values());

        answer1Correct.setToggleGroup(correctAnswerGroup);
        answer2Correct.setToggleGroup(correctAnswerGroup);
        answer3Correct.setToggleGroup(correctAnswerGroup);
        answer4Correct.setToggleGroup(correctAnswerGroup);

        questionListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Question question, boolean empty) {
                super.updateItem(question, empty);
                setText(empty || question == null ? null : question.toString());
            }
        });

        questionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateFormForEditing(newVal);
            }
        });
    }

    public void setController(QuestionClientController controller) {
        this.controller = controller;
        controller.setView(this);

        if (controller.getCurrentTeacher() != null) {
            List<Course> myCourses = controller.getCurrentTeacher().getCourses();
            courseSelector.getItems().setAll(myCourses);
            searchCourseSelector.getItems().setAll(myCourses);
        }

        handleSearchQuestions();
    }

    public void setLoggedInUser(User user) {
        loggedInLabel.setText("Logged in as: " + user.getFullName());
    }

    private User navUser;
    private com.hsts.client.network.ServerConnection navClient;
    private com.hsts.client.controller.LoginClientController navLoginController;

    public void setNavigation(User user, com.hsts.client.network.ServerConnection client,
                               com.hsts.client.controller.LoginClientController loginController) {
        this.navUser = user;
        this.navClient = client;
        this.navLoginController = loginController;
    }

    @FXML
    private void handleBack() {
        NavigationHelper.goToDashboard(loggedInLabel, navUser, navClient, navLoginController);
    }

    @FXML
    private void handleLogout() {
        NavigationHelper.logoutWithConfirmation(loggedInLabel, navClient, navLoginController);
    }

    @FXML
    private void handleSearchQuestions() {
        if (!suppressMessageClear) {
            clearMessages();
        }
        String topic = blankToNull(searchTopicField.getText());
        Difficulty difficulty = searchDifficultySelector.getValue();
        Course course = searchCourseSelector.getValue();
        controller.searchQuestions(topic, difficulty, course != null ? course.getId() : null);
    }

    /**
     * NEW: called by QuestionClientController when the server broadcasts
     * Command.QUESTIONS_CHANGED (another client created, edited, or deleted
     * a question). Re-runs the current search so this client's list stays
     * in sync without the user needing to manually click Search again.
     * Does not touch pendingSelectQuestionId or the status message, since
     * this refresh wasn't caused by anything this window's own user did.
     */
    public void refreshFromServerNotification() {
        String topic = blankToNull(searchTopicField.getText());
        Difficulty difficulty = searchDifficultySelector.getValue();
        Course course = searchCourseSelector.getValue();
        controller.searchQuestions(topic, difficulty, course != null ? course.getId() : null);
    }

    @FXML
    private void handleNewQuestion() {
        clearMessages();
        selectedQuestion = null;
        questionListView.getSelectionModel().clearSelection();
        clearForm();
    }

    @FXML
    private void handleSaveQuestion() {
        clearMessages();

        String text = questionTextField.getText();
        String instructions = instructionsField.getText();
        Difficulty difficulty = difficultySelector.getValue();
        String topic = topicField.getText();
        String imagePath = blankToNull(imagePathField.getText());
        Course selectedCourse = courseSelector.getValue();

        if (text == null || text.isBlank() || difficulty == null || topic == null || topic.isBlank()
                || selectedCourse == null) {
            showError("Question text, difficulty, topic and course are required.");
            return;
        }

        List<QuestionAnswer> answers = collectAnswers();
        if (answers == null) {
            return; // showError already called inside collectAnswers
        }

        String courseId = selectedCourse.getId();

        if (selectedQuestion == null) {
            controller.createQuestion(text, instructions, difficulty, topic, imagePath, courseId, answers);
        } else {
            selectedQuestion.setText(text);
            selectedQuestion.setInstructions(instructions);
            selectedQuestion.setDifficulty(difficulty);
            selectedQuestion.setTopic(topic);
            selectedQuestion.setImagePath(imagePath);
            selectedQuestion.setAnswers(answers);
            controller.editQuestion(selectedQuestion);
        }
    }

    @FXML
    private void handleDeleteQuestion() {
        clearMessages();
        if (selectedQuestion == null) {
            showError("Select a question from the list first.");
            return;
        }
        controller.deleteQuestion(selectedQuestion);
    }

    public void displayQuestions(List<Question> questions) {
        questionListView.getItems().setAll(questions);

        if (pendingSelectQuestionId != null) {
            for (Question q : questionListView.getItems()) {
                if (q.getQuestionId().equals(pendingSelectQuestionId)) {
                    questionListView.getSelectionModel().select(q);
                    questionListView.scrollTo(q);
                    break;
                }
            }
            pendingSelectQuestionId = null;
        }

        // FIX: suppressMessageClear used to be reset back to false right
        // after firing the (asynchronous) search request in onQuestionSaved,
        // before this method - the actual response handler - ever ran. That
        // meant the re-selection above could fire populateFormForEditing and
        // wipe the just-set success message before the user ever saw it.
        // Resetting it here instead, after the re-selection that needs
        // protecting has actually happened, removes that race completely.
        suppressMessageClear = false;
    }

    public void onQuestionSaved(Question question) {
        pendingSelectQuestionId = question.getQuestionId();

        // Clear any active filters - otherwise a saved question that doesn't
        // match the current search (e.g. different course/topic) would
        // vanish from the list and look like the save silently failed.
        searchTopicField.clear();
        searchDifficultySelector.setValue(null);
        searchCourseSelector.setValue(null);

        // FIX: previously this message was set after handleSearchQuestions()
        // returned, but that call is asynchronous - it fires the request and
        // returns immediately, while the actual response (and the resulting
        // re-selection of this question) arrives later. suppressMessageClear
        // used to be reset to false right here too, before that delayed
        // response ever ran - so when it eventually re-selected the question
        // and fired the selection listener, nothing was protecting the
        // message anymore and it got wiped almost instantly. The message is
        // now set first, and suppressMessageClear is reset inside
        // displayQuestions() instead, after the re-selection it's meant to
        // protect has actually happened - see displayQuestions() below.
        statusLabel.setText("Question " + question.getQuestionId() + " saved successfully.");

        suppressMessageClear = true;
        handleSearchQuestions();
    }

    public void onQuestionDeleted() {
        selectedQuestion = null;
        clearForm();

        suppressMessageClear = true;
        handleSearchQuestions();
        suppressMessageClear = false;

        statusLabel.setText("Question deleted successfully.");
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }

    private void populateFormForEditing(Question question) {
        if (!suppressMessageClear) {
            clearMessages();
        }
        selectedQuestion = question;

        questionTextField.setText(question.getText());
        instructionsField.setText(question.getInstructions());
        difficultySelector.setValue(question.getDifficulty());
        topicField.setText(question.getTopic());
        courseSelector.setValue(findCourseById(question.getCourseId()));
        imagePathField.setText(question.getImagePath());

        List<QuestionAnswer> answers = question.getAnswers();
        TextField[] fields = {answer1Field, answer2Field, answer3Field, answer4Field};
        RadioButton[] radios = {answer1Correct, answer2Correct, answer3Correct, answer4Correct};
        for (int i = 0; i < 4 && i < answers.size(); i++) {
            fields[i].setText(answers.get(i).getText());
            radios[i].setSelected(answers.get(i).isCorrect());
        }
    }

    private Course findCourseById(String courseId) {
        return courseSelector.getItems().stream()
                .filter(c -> c.getId().equals(courseId))
                .findFirst().orElse(null);
    }

    private List<QuestionAnswer> collectAnswers() {
        TextField[] fields = {answer1Field, answer2Field, answer3Field, answer4Field};
        RadioButton[] radios = {answer1Correct, answer2Correct, answer3Correct, answer4Correct};

        for (TextField field : fields) {
            if (field.getText() == null || field.getText().isBlank()) {
                showError("All four answers must be filled in.");
                return null;
            }
        }

        Toggle selected = correctAnswerGroup.getSelectedToggle();
        if (selected == null) {
            showError("Select which answer is correct.");
            return null;
        }

        List<QuestionAnswer> answers = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            answers.add(new QuestionAnswer(fields[i].getText(), radios[i].isSelected()));
        }
        return answers;
    }

    private void clearForm() {
        questionTextField.clear();
        instructionsField.clear();
        difficultySelector.setValue(null);
        topicField.clear();
        courseSelector.setValue(null);
        imagePathField.clear();
        for (TextField f : new TextField[]{answer1Field, answer2Field, answer3Field, answer4Field}) {
            f.clear();
        }
        correctAnswerGroup.selectToggle(null);
    }

    private void clearMessages() {
        statusLabel.setText("");
        errorLabel.setText("");
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}