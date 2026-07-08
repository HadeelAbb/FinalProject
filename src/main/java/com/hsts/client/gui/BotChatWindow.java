package com.hsts.client.gui;

import com.hsts.client.controller.BotChatClientController;
import com.hsts.shared.model.BotInteraction;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Student;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.util.List;

public class BotChatWindow {

    @FXML private ComboBox<Course> courseSelector;
    @FXML private ListView<String> historyView;
    @FXML private TextArea questionField;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private BotChatClientController controller;

    public void init(BotChatClientController controller, Student student) {
        this.controller = controller;
        controller.setCurrentStudent(student);
        controller.setView(this);

        courseSelector.getItems().setAll(student.getCourses());
        if (!student.getCourses().isEmpty()) {
            courseSelector.getSelectionModel().selectFirst();
        }

        controller.refreshHistory();
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
        controller.ask(course.getId(), questionField.getText());
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        controller.refreshHistory();
    }

    public void onAnswerReceived(BotInteraction interaction) {
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
        errorLabel.setText(message);
        statusLabel.setText("");
    }
}
