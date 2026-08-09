package com.hsts.client.gui;

import com.hsts.client.controller.ExamTimeClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamExecution;
import com.hsts.shared.model.ExamStatus;
import com.hsts.shared.model.ExecutionStats;
import com.hsts.shared.model.Teacher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExamTimeWindow {

    private static final DateTimeFormatter SEND_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ListView<Exam> examListView;
    @FXML private Spinner<Integer> minutesSpinner;
    @FXML private Button extendButton;
    @FXML private ListView<ExamExecution> executionListView;
    @FXML private DatePicker newExecStartDatePicker;
    @FXML private TextField newExecStartTimeField;
    @FXML private DatePicker newExecEndDatePicker;
    @FXML private TextField newExecEndTimeField;
    @FXML private Button openExecutionButton;
    @FXML private Label executionStatsLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private ExamTimeClientController controller;
    private Teacher navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;
    private Exam selectedExam;
    private ExamExecution selectedExecution;

    public void init(ExamTimeClientController controller, Teacher teacher,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = teacher;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentTeacher(teacher);
        controller.setView(this);

        examListView.setPlaceholder(new Label("No approved exams to manage."));
        examListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Exam exam, boolean empty) {
                super.updateItem(exam, empty);
                setText(empty || exam == null ? null
                        : "[" + exam.getExamId() + "] " + exam.getTitle() + " (" + exam.getStatus() + ") - "
                          + exam.getDurationMinutes() + " min");
            }
        });
        executionListView.setPlaceholder(new Label("Select an exam to see its executions."));
        executionListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ExamExecution execution, boolean empty) {
                super.updateItem(execution, empty);
                setText(empty || execution == null ? null
                        : "Code " + execution.getExecutionCode()
                          + (execution.getExtraMinutesGranted() > 0
                             ? " (+" + execution.getExtraMinutesGranted() + " min granted)" : ""));
            }
        });
        minutesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 10, 1));

        examListView.getSelectionModel().selectedItemProperty().addListener((obs, oldExam, newExam) -> {
            selectedExam = newExam;
            selectedExecution = null;
            executionListView.getItems().clear();
            executionStatsLabel.setText("");
            if (newExam != null) {
                controller.loadExecutions(newExam.getExamId());
            }
        });

        executionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldExec, newExec) -> {
            selectedExecution = newExec;
            if (newExec != null) {
                executionStatsLabel.setText("Loading stats...");
                controller.loadExecutionStats(newExec.getExecutionId());
            } else {
                executionStatsLabel.setText("");
            }
        });

        controller.refreshMyExams();
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
    void handleRefresh(ActionEvent event) {
        controller.refreshMyExams();
    }

    @FXML
    void handleExtend(ActionEvent event) {
        if (selectedExam == null) {
            showError("Select an exam first.");
            return;
        }
        if (selectedExecution == null) {
            showError("Select a specific execution on the right to extend (an exam can have several).");
            return;
        }
        int minutes = minutesSpinner.getValue();
        if (!NavigationHelper.confirm("Add " + minutes + " minutes to execution " + selectedExecution.getExecutionCode()
                + "? This applies to students currently taking it AND anyone starting it from now on.")) {
            return;
        }
        extendButton.setDisable(true);
        controller.extend(selectedExam.getExamId(), selectedExecution.getExecutionId(), minutes);
    }

    /** Combines a DatePicker's date with a "HH:mm" text field into the "dd-MM-yyyy HH:mm" string the server expects. */
    private String combineDateAndTime(DatePicker datePicker, TextField timeField, String fieldLabel) {
        LocalDate date = datePicker.getValue();
        String timeText = timeField.getText() != null ? timeField.getText().trim() : "";
        if (date == null || timeText.isEmpty()) {
            showError(fieldLabel + " date and time are both required.");
            return null;
        }
        LocalTime time;
        try {
            time = LocalTime.parse(timeText, TIME_FORMAT);
        } catch (Exception e) {
            showError(fieldLabel + " time must be in the format HH:mm (24-hour), e.g. 09:00 or 14:30.");
            return null;
        }
        return LocalDateTime.of(date, time).format(SEND_FORMAT);
    }

    @FXML
    void handleOpenExecution(ActionEvent event) {
        if (selectedExam == null) {
            showError("Select an exam first.");
            return;
        }
        String start = combineDateAndTime(newExecStartDatePicker, newExecStartTimeField, "Open");
        if (start == null) {
            return;
        }
        String end = combineDateAndTime(newExecEndDatePicker, newExecEndTimeField, "Close");
        if (end == null) {
            return;
        }
        if (!NavigationHelper.confirm("Open a new sitting of \"" + selectedExam.getTitle()
                + "\"? Students will need a fresh execution code to take this new sitting.")) {
            return;
        }
        openExecutionButton.setDisable(true);
        controller.createExecution(selectedExam.getExamId(), start, end);
    }

    public void displayExams(List<Exam> exams) {
        examListView.getItems().setAll(exams.stream().filter(e -> e.getStatus() == ExamStatus.APPROVED).toList());
        errorLabel.setText("");
        extendButton.setDisable(false);
    }

    public void onExtended(ExamExecution execution, String message) {
        extendButton.setDisable(false);
        statusLabel.setText(message);
        if (selectedExam != null) {
            controller.loadExecutions(selectedExam.getExamId());
        }
    }

    public void onExecutionCreated(ExamExecution execution) {
        openExecutionButton.setDisable(false);
        newExecStartDatePicker.setValue(null);
        newExecStartTimeField.clear();
        newExecEndDatePicker.setValue(null);
        newExecEndTimeField.clear();
        statusLabel.setText("New execution opened - code: " + execution.getExecutionCode());
        if (selectedExam != null) {
            controller.loadExecutions(selectedExam.getExamId());
        }
    }

    public void displayExecutions(List<ExamExecution> executions) {
        executionListView.getItems().setAll(executions);
    }

    public void displayExecutionStats(ExecutionStats stats) {
        executionStatsLabel.setText(String.format(
                "Started: %d - Finished themselves: %d - Didn't manage (timed out): %d",
                stats.getTotalStarted(), stats.getFinishedThemselves(), stats.getDidntManage()));
    }

    public void showError(String message) {
        extendButton.setDisable(false);
        openExecutionButton.setDisable(false);
        errorLabel.setText(message);
        statusLabel.setText("");
    }
}