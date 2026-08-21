package com.hsts.client.gui;

import com.hsts.client.controller.LoginClientController;
import com.hsts.client.controller.PrincipalQuestionBankClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Principal;
import com.hsts.shared.model.PrincipalQuestionDetailFormatter;
import com.hsts.shared.model.Question;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.TreeSet;

public class PrincipalQuestionBankWindow {

    private static final String ALL_COURSES = "All courses";

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ComboBox<String> courseCombo;
    @FXML private TextField topicField;
    @FXML private ComboBox<Difficulty> difficultyCombo;
    @FXML private Button searchButton;
    @FXML private TableView<Question> questionTable;
    @FXML private TextArea detailArea;
    @FXML private ImageView illustrationView;
    @FXML private Label errorLabel;

    private PrincipalQuestionBankClientController controller;
    private Principal navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;
    private final TreeSet<String> knownCourses = new TreeSet<>();

    public void init(PrincipalQuestionBankClientController controller, Principal principal,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = principal;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setView(this);

        difficultyCombo.getItems().setAll(Difficulty.values());
        courseCombo.getItems().setAll(ALL_COURSES);
        courseCombo.getSelectionModel().select(ALL_COURSES);
        setupTable();
        questionTable.setPlaceholder(new Label(PrincipalQuestionDetailFormatter.emptyBankMessage()));
        questionTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldQ, newQ) -> showDetail(newQ));
        detailArea.setText("Select a question to view its details.");
        controller.search(null, null, null);
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
    void handleSearch(ActionEvent event) {
        errorLabel.setText("");
        controller.search(selectedCourseId(), blankToNull(topicField.getText()), difficultyCombo.getValue());
    }

    public void displayQuestions(List<Question> questions) {
        List<Question> rows = questions != null ? questions : List.of();
        if (selectedCourseId() == null) {
            knownCourses.clear();
            for (Question question : rows) {
                if (question.getCourseId() != null && !question.getCourseId().isBlank()) {
                    knownCourses.add(question.getCourseId());
                }
            }
            String previous = courseCombo.getValue();
            courseCombo.getItems().setAll(ALL_COURSES);
            courseCombo.getItems().addAll(knownCourses);
            if (previous != null && courseCombo.getItems().contains(previous)) {
                courseCombo.getSelectionModel().select(previous);
            } else {
                courseCombo.getSelectionModel().select(ALL_COURSES);
            }
        }
        questionTable.getItems().setAll(rows);
        errorLabel.setText("");
        if (rows.isEmpty()) {
            detailArea.setText(PrincipalQuestionDetailFormatter.emptyBankMessage());
            QuestionIllustrationView.apply(illustrationView, (Question) null);
        } else {
            questionTable.getSelectionModel().select(0);
        }
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }

    private void setupTable() {
        TableColumn<Question, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getQuestionId())));
        idCol.setPrefWidth(90);
        TableColumn<Question, String> versionCol = new TableColumn<>("Version");
        versionCol.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getVersionNumber())));
        versionCol.setPrefWidth(70);
        TableColumn<Question, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().versionStatusLabel()));
        statusCol.setPrefWidth(90);
        TableColumn<Question, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getCourseId())));
        courseCol.setPrefWidth(80);
        TableColumn<Question, String> topicCol = new TableColumn<>("Topic");
        topicCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getTopic())));
        topicCol.setPrefWidth(120);
        TableColumn<Question, String> difficultyCol = new TableColumn<>("Difficulty");
        difficultyCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDifficulty() != null ? c.getValue().getDifficulty().toString() : ""));
        difficultyCol.setPrefWidth(90);
        TableColumn<Question, String> textCol = new TableColumn<>("Question");
        textCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getText())));
        textCol.setPrefWidth(280);
        questionTable.getColumns().setAll(idCol, versionCol, statusCol, courseCol, topicCol, difficultyCol, textCol);
        questionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void showDetail(Question question) {
        if (question == null) {
            detailArea.setText(questionTable.getItems().isEmpty()
                    ? PrincipalQuestionDetailFormatter.emptyBankMessage()
                    : "Select a question to view its details.");
            QuestionIllustrationView.apply(illustrationView, (Question) null);
            return;
        }
        detailArea.setText(PrincipalQuestionDetailFormatter.format(question));
        QuestionIllustrationView.apply(illustrationView, question);
    }

    private String selectedCourseId() {
        String selected = courseCombo.getValue();
        if (selected == null || ALL_COURSES.equals(selected) || selected.isBlank()) {
            return null;
        }
        return selected;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
