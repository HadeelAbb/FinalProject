package com.hsts.client.gui;

import com.hsts.client.controller.BotChatClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.BotInteraction;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Student;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.util.List;

public class BotChatWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ComboBox<Course> courseSelector;
    @FXML private ListView<String> historyView;
    @FXML private TextArea questionField;
    @FXML private Button askButton;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private BotChatClientController controller;
    private Student navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;

    public void init(BotChatClientController controller, Student student,
                      ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = student;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentStudent(student);
        controller.setView(this);

        historyView.setPlaceholder(new Label("No questions asked yet."));
        courseSelector.getItems().setAll(student.getCourses());
        if (!student.getCourses().isEmpty()) {
            courseSelector.getSelectionModel().selectFirst();
        }

        controller.refreshHistory();
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
    void handleAsk(ActionEvent event) {
        errorLabel.setText("");
        Course course = courseSelector.getValue();
        if (course == null) {
            showError("Select a course first.");
            return;
        }
        if (questionField.getText() == null || questionField.getText().isBlank()) {
            showError("Type a question first.");
            return;
        }
        askButton.setDisable(true);
        statusLabel.setText("Asking the bot...");
        controller.ask(course.getId(), questionField.getText());
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        controller.refreshHistory();
    }

    public void onAnswerReceived(BotInteraction interaction) {
        askButton.setDisable(false);
        statusLabel.setText("Answered.");
        questionField.clear();
        errorLabel.setText("");
    }

    public void displayHistory(List<BotInteraction> history) {
        List<String> lines = history.stream()
                .sorted((a, b) -> b.getAskedAt().compareTo(a.getAskedAt()))
                .map(i -> "[" + i.getCourseId() + "] Q: " + i.getQuestion() + "\n  A: " + i.getAnswer())
                .toList();
        historyView.getItems().setAll(lines);
    }

    public void showError(String message) {
        askButton.setDisable(false);
        errorLabel.setText(message);
        statusLabel.setText("");
    }
}
