package com.hsts.client.gui;

import com.hsts.client.controller.BotChatClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.BotInteraction;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Student;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

public class BotChatWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ComboBox<Course> courseSelector;
    @FXML private ListView<BotInteraction> historyView;
    @FXML private TextArea questionField;
    @FXML private Button askButton;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private BotChatClientController controller;
    private Student navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;
    private final List<BotInteraction> fullHistory = new ArrayList<>();

    public void init(BotChatClientController controller, Student student,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = student;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentStudent(student);
        controller.setView(this);

        setupCourseSelector(student);
        setupChatBubbleCellFactory();

        controller.refreshHistory();
    }

    private void setupCourseSelector(Student student) {
        courseSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getId() + " - " + item.getName());
            }
        });
        courseSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getId() + " - " + item.getName());
            }
        });

        courseSelector.getItems().setAll(student.getCourses());
        if (!student.getCourses().isEmpty()) {
            courseSelector.getSelectionModel().selectFirst();
        }

        courseSelector.setOnAction(e -> filterHistoryForCurrentCourse());
    }

    private void setupChatBubbleCellFactory() {
        historyView.setPlaceholder(new Label("No conversation history yet for this course."));
        historyView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BotInteraction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                VBox container = new VBox(8);
                container.setPadding(new Insets(10));
                container.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #dfe6e9; -fx-border-radius: 8;");
                container.prefWidthProperty().bind(historyView.widthProperty().subtract(40));

                // Student Question Bubble
                Text qHeader = new Text("You asked: ");
                qHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                qHeader.setFill(Color.web("#2980b9"));

                Text qText = new Text(item.getQuestion() != null ? item.getQuestion() : "");
                qText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
                qText.setFill(Color.web("#2c3e50"));

                TextFlow questionFlow = new TextFlow(qHeader, qText);
                questionFlow.setStyle("-fx-background-color: #ebf5fb; -fx-padding: 8; -fx-background-radius: 6;");

                // Bot Answer Section
                Text aHeader = new Text("🤖 Study Bot:\n");
                aHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                aHeader.setFill(Color.web("#27ae60"));

                String cleanAnswer = cleanMarkdown(item.getAnswer());
                Text aText = new Text(cleanAnswer);
                aText.setFont(Font.font("Segoe UI", 13));
                aText.setFill(Color.web("#34495e"));

                TextFlow answerFlow = new TextFlow(aHeader, aText);
                answerFlow.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8; -fx-background-radius: 6;");

                container.getChildren().addAll(questionFlow, answerFlow);
                setGraphic(container);
                setText(null);
            }
        });
    }

    private String cleanMarkdown(String raw) {
        if (raw == null) return "";
        return raw.replace("**", "").replace("###", "").trim();
    }

    private void filterHistoryForCurrentCourse() {
        Course selected = courseSelector.getValue();
        if (selected == null) {
            historyView.getItems().clear();
            return;
        }

        List<BotInteraction> filtered = fullHistory.stream()
                .filter(i -> selected.getId().equalsIgnoreCase(i.getCourseId()))
                .sorted((a, b) -> {
                    if (a.getAskedAt() == null || b.getAskedAt() == null) return 0;
                    return a.getAskedAt().compareTo(b.getAskedAt()); // Oldest first, newest at the bottom
                })
                .toList();

        historyView.getItems().setAll(filtered);

        // Automatically scroll to the latest message at the bottom
        if (!filtered.isEmpty()) {
            historyView.scrollTo(filtered.size() - 1);
        }
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
        controller.ask(course.getId(), questionField.getText().trim());
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        controller.refreshHistory();
    }

    public void onAnswerReceived(BotInteraction interaction) {
        Platform.runLater(() -> {
            askButton.setDisable(false);
            statusLabel.setText("");
            questionField.clear();
            errorLabel.setText("");
            controller.refreshHistory();
        });
    }

    public void displayHistory(List<BotInteraction> history) {
        Platform.runLater(() -> {
            fullHistory.clear();
            if (history != null) {
                fullHistory.addAll(history);
            }
            filterHistoryForCurrentCourse();
        });
    }

    public void showError(String message) {
        Platform.runLater(() -> {
            askButton.setDisable(false);
            errorLabel.setText(message);
            statusLabel.setText("");
        });
    }
}