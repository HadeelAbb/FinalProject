package com.hsts.client.gui;

import com.hsts.client.controller.QuestionClientController;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.QuestionIllustration;
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
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

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
    private Button browseImageButton;
    @FXML
    private Label imageFileLabel;
    @FXML
    private ImageView illustrationView;
    @FXML
    private Label versionLabel;

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
    private byte[] pendingImageData;
    private String pendingImageFilename;
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
        saveButton.setDisable(false);
        if (versionLabel != null) {
            versionLabel.setText("");
        }
        if (browseImageButton != null) {
            browseImageButton.setDisable(false);
        }
    }

    @FXML
    private void handleBrowseImage() {
        if (selectedQuestion != null && !selectedQuestion.isLatest()) {
            showError("Only the current version of this question can be edited.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose question illustration");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("PNG or JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(browseImageButton != null && browseImageButton.getScene() != null
                ? browseImageButton.getScene().getWindow() : null);
        if (file == null) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String error = QuestionIllustration.validate(bytes, file.getName());
            if (error != null) {
                showError(error);
                return;
            }
            pendingImageData = bytes;
            pendingImageFilename = QuestionIllustration.filenameForStorage(bytes, file.getName());
            showIllustration(pendingImageData, pendingImageFilename);
        } catch (Exception e) {
            showError("Could not read the selected image.");
        }
    }

    @FXML
    private void handleSaveQuestion() {
        clearMessages();

        String text = trimToNull(questionTextField.getText());
        String instructions = trimToNull(instructionsField.getText());
        Difficulty difficulty = difficultySelector.getValue();
        String topic = trimToNull(topicField.getText());
        Course selectedCourse = courseSelector.getValue();

        if (text == null || difficulty == null || topic == null || selectedCourse == null) {
            showError("Question text, difficulty, topic and course are required.");
            return;
        }
        if (text.length() > 500) {
            showError("Question text is too long (max 500 characters).");
            return;
        }
        if (topic.length() > 100) {
            showError("Topic is too long (max 100 characters).");
            return;
        }

        List<QuestionAnswer> answers = collectAnswers();
        if (answers == null) {
            return; // showError already called inside collectAnswers
        }

        if (selectedQuestion != null && !selectedQuestion.isLatest()) {
            showError("Only the current version of this question can be edited.");
            return;
        }
        String courseId = selectedCourse.getId();

        if (selectedQuestion == null) {
            controller.createQuestion(text, instructions, difficulty, topic, pendingImageFilename,
                    pendingImageData, courseId, answers);
        } else {
            selectedQuestion.setText(text);
            selectedQuestion.setInstructions(instructions);
            selectedQuestion.setDifficulty(difficulty);
            selectedQuestion.setTopic(topic);
            selectedQuestion.setImagePath(pendingImageFilename);
            selectedQuestion.setImageData(pendingImageData);
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

        // Clear topic/difficulty filters so a just-saved question is not hidden
        // by an unrelated search. Keep (or set) the course filter to the saved
        // question's real course id (e.g. CS101). Searching with a null course
        // currently falls back to a leftover numeric default ("11") on the
        // server, which would make the new row look like it never persisted.
        searchTopicField.clear();
        searchDifficultySelector.setValue(null);
        searchCourseSelector.setValue(findCourseById(question.getCourseId()));

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
        statusLabel.setText(question.getVersionNumber() > 1
                ? "Question updated. Version " + question.getVersionNumber()
                + " created; previous version was preserved."
                : "Question " + question.getQuestionId() + " saved successfully.");

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
        pendingImageData = question.getImageData();
        pendingImageFilename = question.getImagePath();
        showIllustration(pendingImageData, pendingImageFilename);
        if (versionLabel != null) {
            versionLabel.setText("Version " + question.getVersionNumber()
                    + " — " + question.versionStatusLabel());
        }
        saveButton.setDisable(!question.isLatest());
        if (browseImageButton != null) {
            browseImageButton.setDisable(!question.isLatest());
        }
        if (!question.isLatest() && !suppressMessageClear) {
            statusLabel.setText("This version is historical (read-only). Edit the current version to create a new one.");
        }

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

        String[] trimmed = new String[4];
        for (int i = 0; i < 4; i++) {
            String value = trimToNull(fields[i].getText());
            if (value == null) {
                showError("All four answers must be filled in.");
                return null;
            }
            if (value.length() > 200) {
                showError("Answer " + (i + 1) + " is too long (max 200 characters).");
                return null;
            }
            trimmed[i] = value;
        }

        Set<String> seen = new HashSet<>();
        for (String value : trimmed) {
            if (!seen.add(value.toLowerCase())) {
                showError("Answers must be different from each other.");
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
            answers.add(new QuestionAnswer(trimmed[i], radios[i].isSelected()));
        }
        return answers;
    }

    private void clearForm() {
        questionTextField.clear();
        instructionsField.clear();
        difficultySelector.setValue(null);
        topicField.clear();
        courseSelector.setValue(null);
        pendingImageData = null;
        pendingImageFilename = null;
        showIllustration(null, null);
        if (versionLabel != null) {
            versionLabel.setText("");
        }
        saveButton.setDisable(false);
        for (TextField f : new TextField[]{answer1Field, answer2Field, answer3Field, answer4Field}) {
            f.clear();
        }
        correctAnswerGroup.selectToggle(null);
    }

    private void showIllustration(byte[] imageData, String filename) {
        if (imageFileLabel != null) {
            imageFileLabel.setText(QuestionIllustration.hasData(imageData) && filename != null && !filename.isBlank()
                    ? filename : (QuestionIllustration.hasData(imageData) ? "Illustration attached" : "No illustration"));
        }
        QuestionIllustrationView.apply(illustrationView, imageData);
    }

    private void clearMessages() {
        statusLabel.setText("");
        errorLabel.setText("");
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /** Like blankToNull, but also trims surrounding whitespace from raw text input. */
    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}