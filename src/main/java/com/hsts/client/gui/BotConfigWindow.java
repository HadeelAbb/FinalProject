package com.hsts.client.gui;

import com.hsts.client.controller.BotConfigClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.CourseBotConfig;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.model.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class BotConfigWindow {

    private final ServerConnection serverConnection;
    private final User currentUser;
    private final LoginClientController loginController;
    private final BotConfigClientController controller;

    private ComboBox<Course> courseComboBox;
    private TextField botNameField;
    private TextArea knowledgeSourcesArea;
    private CheckBox activeCheckBox;
    private Label lastUpdatedLabel;
    private Label statusLabel;
    private Button saveButton;

    public BotConfigWindow(ServerConnection serverConnection, User currentUser, LoginClientController loginController) {
        this.serverConnection = serverConnection;
        this.currentUser = currentUser;
        this.loginController = loginController;
        this.controller = new BotConfigClientController(serverConnection);
        this.controller.setView(this);
    }

    public void show(Stage stage) {
        stage.setTitle("Study Bot Configuration - Teacher Panel");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f6f9;");

        // Header
        Label headerLabel = new Label("🤖 Study Bot Curriculum & Knowledge Configuration");
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        headerLabel.setTextFill(Color.web("#2c3e50"));

        Label subHeader = new Label("Define syllabus notes, textbook topics, and toggle bot availability for your courses.");
        subHeader.setFont(Font.font("Segoe UI", 12));
        subHeader.setTextFill(Color.web("#7f8c8d"));

        // Form Grid
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER_LEFT);

        // Course Selector
        Label courseLabel = new Label("Select Course:");
        courseLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        courseComboBox = new ComboBox<>();
        courseComboBox.setPrefWidth(300);

        if (currentUser instanceof Teacher teacher && teacher.getCourses() != null) {
            courseComboBox.getItems().addAll(teacher.getCourses());
        }
        courseComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getId() + " - " + item.getName());
            }
        });
        courseComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getId() + " - " + item.getName());
            }
        });

        // Bot Display Name
        Label nameLabel = new Label("Bot Name:");
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        botNameField = new TextField();
        botNameField.setPromptText("e.g. CS101 Study Assistant");

        // Active State Checkbox
        Label activeLabel = new Label("Bot Status:");
        activeLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        activeCheckBox = new CheckBox("Active (Enabled for enrolled students)");
        activeCheckBox.setSelected(true);

        // Knowledge Sources Text Area
        Label sourcesLabel = new Label("Knowledge Sources & Focus Areas:");
        sourcesLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        knowledgeSourcesArea = new TextArea();
        knowledgeSourcesArea.setPromptText("Enter syllabus topics, chapter summaries, key concepts, or question bank references that the AI should focus on when answering students...");
        knowledgeSourcesArea.setWrapText(true);
        knowledgeSourcesArea.setPrefRowCount(8);

        // Last Updated Metadata
        lastUpdatedLabel = new Label("Last updated: -");
        lastUpdatedLabel.setFont(Font.font("Segoe UI", 11));
        lastUpdatedLabel.setTextFill(Color.web("#7f8c8d"));

        // Add to grid
        grid.add(courseLabel, 0, 0);
        grid.add(courseComboBox, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(botNameField, 1, 1);
        grid.add(activeLabel, 0, 2);
        grid.add(activeCheckBox, 1, 2);
        grid.add(sourcesLabel, 0, 3);
        grid.add(knowledgeSourcesArea, 1, 3);
        grid.add(new Label(""), 0, 4);
        grid.add(lastUpdatedLabel, 1, 4);

        // Action Buttons
        saveButton = new Button("💾 Save Configuration");
        saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        saveButton.setDisable(true);

        Button backButton = new Button("Back to Dashboard");
        backButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 8 16;");
        // FIXED: Passing node, user, connection, and loginController
        backButton.setOnAction(e -> NavigationHelper.goToDashboard(backButton, currentUser, serverConnection, loginController));

        HBox buttonBox = new HBox(12, saveButton, backButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("");
        statusLabel.setFont(Font.font("Segoe UI", 12));

        // Event Handlers
        courseComboBox.setOnAction(e -> {
            Course selected = courseComboBox.getValue();
            if (selected != null) {
                loadConfig(selected.getId());
            }
        });

        saveButton.setOnAction(e -> saveConfig());

        if (!courseComboBox.getItems().isEmpty()) {
            courseComboBox.getSelectionModel().selectFirst();
            loadConfig(courseComboBox.getValue().getId());
        }

        root.getChildren().addAll(headerLabel, subHeader, new Separator(), grid, buttonBox, statusLabel);

        Scene scene = new Scene(root, 700, 580);
        stage.setScene(scene);
        stage.show();
    }

    private void loadConfig(String courseId) {
        statusLabel.setTextFill(Color.web("#2980b9"));
        statusLabel.setText("Loading configuration for " + courseId + "...");
        saveButton.setDisable(true);
        controller.loadBotConfig(courseId);
    }

    private void saveConfig() {
        Course selected = courseComboBox.getValue();
        if (selected == null) {
            showError("Please select a course first.");
            return;
        }

        String name = botNameField.getText().trim();
        if (name.isEmpty()) {
            showError("Bot name cannot be empty.");
            return;
        }

        String sources = knowledgeSourcesArea.getText().trim();
        boolean active = activeCheckBox.isSelected();

        statusLabel.setTextFill(Color.web("#2980b9"));
        statusLabel.setText("Saving configuration...");
        saveButton.setDisable(true);

        controller.saveBotConfig(selected.getId(), name, sources, active);
    }

    public void onConfigLoaded(CourseBotConfig config) {
        Platform.runLater(() -> {
            botNameField.setText(config.getBotName() != null ? config.getBotName() : "");
            knowledgeSourcesArea.setText(config.getKnowledgeSources() != null ? config.getKnowledgeSources() : "");
            activeCheckBox.setSelected(config.isActive());

            String updatedBy = config.getLastUpdatedBy() != null ? config.getLastUpdatedBy() : "Default";
            String updatedAt = config.getLastUpdatedAt() != null ? config.getLastUpdatedAt().toString().replace('T', ' ') : "Never";
            lastUpdatedLabel.setText("Last updated by: " + updatedBy + " | At: " + updatedAt);

            statusLabel.setText("");
            saveButton.setDisable(false);
        });
    }

    public void onConfigSaved(CourseBotConfig config) {
        Platform.runLater(() -> {
            statusLabel.setTextFill(Color.web("#27ae60"));
            statusLabel.setText("✓ Configuration saved successfully!");
            saveButton.setDisable(false);

            String updatedBy = config.getLastUpdatedBy() != null ? config.getLastUpdatedBy() : currentUser.getId();
            lastUpdatedLabel.setText("Last updated by: " + updatedBy + " | Just now");
        });
    }

    public void showError(String message) {
        Platform.runLater(() -> {
            statusLabel.setTextFill(Color.web("#c0392b"));
            statusLabel.setText("Error: " + message);
            saveButton.setDisable(false);
        });
    }
}