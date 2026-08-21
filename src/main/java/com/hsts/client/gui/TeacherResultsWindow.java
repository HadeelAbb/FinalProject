package com.hsts.client.gui;

import com.hsts.client.controller.LoginClientController;
import com.hsts.client.controller.TeacherResultsClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.GradeHistogramCalculator;
import com.hsts.shared.model.Teacher;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TeacherResultsWindow {

    private static final DateTimeFormatter SUBMITTED_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ListView<Exam> examListView;
    @FXML private Button refreshButton;
    @FXML private Label examTitleLabel;
    @FXML private Label emptyLabel;
    @FXML private TableView<ExamAnswer> resultsTable;
    @FXML private BarChart<String, Number> histogramChart;
    @FXML private Label errorLabel;

    private TeacherResultsClientController controller;
    private Teacher navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;
    private Exam selectedExam;

    public void init(TeacherResultsClientController controller, Teacher teacher,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = teacher;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentTeacher(teacher);
        controller.setView(this);

        examListView.setPlaceholder(new Label("You have not created any exams yet."));
        examListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Exam exam, boolean empty) {
                super.updateItem(exam, empty);
                setText(empty || exam == null ? null
                        : "[" + exam.getExamId() + "] v" + exam.getVersionNumber() + " "
                          + exam.versionStatusLabel() + " " + exam.getTitle()
                          + " (" + exam.getCourseId() + ")");
            }
        });
        examListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldExam, newExam) -> selectExam(newExam));

        setupTableColumns();
        resultsTable.setPlaceholder(new Label("No results available for this exam."));
        histogramChart.setAnimated(false);
        displayResults(List.of());

        controller.refreshMyExams();
    }

    private void setupTableColumns() {
        TableColumn<ExamAnswer, String> studentCol = new TableColumn<>("Student");
        studentCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStudentId() != null ? c.getValue().getStudentId() : ""));
        studentCol.setPrefWidth(110);

        TableColumn<ExamAnswer, String> autoCol = new TableColumn<>("Auto Grade");
        autoCol.setCellValueFactory(c -> new SimpleStringProperty(formatScore(c.getValue().getAutoScore())));
        autoCol.setPrefWidth(90);

        TableColumn<ExamAnswer, String> finalCol = new TableColumn<>("Final Grade");
        finalCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isGradeConfirmed() ? formatScore(c.getValue().getFinalScore()) : "—"));
        finalCol.setPrefWidth(90);

        TableColumn<ExamAnswer, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isGradeConfirmed() ? "Confirmed" : "Pending"));
        statusCol.setPrefWidth(90);

        TableColumn<ExamAnswer, String> submittedCol = new TableColumn<>("Submitted");
        submittedCol.setCellValueFactory(c -> new SimpleStringProperty(formatSubmitted(c.getValue())));
        submittedCol.setPrefWidth(140);

        resultsTable.getColumns().setAll(studentCol, autoCol, finalCol, statusCol, submittedCol);
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void selectExam(Exam exam) {
        selectedExam = exam;
        errorLabel.setText("");
        if (exam == null) {
            examTitleLabel.setText("");
            emptyLabel.setText("");
            displayResults(List.of());
            return;
        }
        examTitleLabel.setText("Exam: " + exam.getTitle() + " v" + exam.getVersionNumber()
                + " " + exam.versionStatusLabel() + " (" + exam.getCourseId() + ")");
        controller.loadExamResults(exam.getExamId());
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
        errorLabel.setText("");
        controller.refreshMyExams();
        if (selectedExam != null) {
            controller.loadExamResults(selectedExam.getExamId());
        }
    }

    public void displayExams(List<Exam> exams) {
        Exam previouslySelected = selectedExam;
        examListView.getItems().setAll(exams);
        errorLabel.setText("");
        if (previouslySelected != null) {
            for (Exam exam : exams) {
                if (exam.equals(previouslySelected)) {
                    examListView.getSelectionModel().select(exam);
                    return;
                }
            }
        }
    }

    public void displayResults(List<ExamAnswer> results) {
        List<ExamAnswer> rows = results != null ? results : List.of();
        resultsTable.getItems().setAll(rows);
        if (selectedExam == null) {
            emptyLabel.setText("");
        } else if (rows.isEmpty()) {
            emptyLabel.setText("No results available for this exam.");
        } else {
            emptyLabel.setText("");
        }
        updateHistogram(rows);
    }

    private void updateHistogram(List<ExamAnswer> results) {
        int[] counts = GradeHistogramCalculator.countsFromAnswers(results);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Students");
        for (int i = 0; i < GradeHistogramCalculator.BUCKET_LABELS.length; i++) {
            series.getData().add(new XYChart.Data<>(GradeHistogramCalculator.BUCKET_LABELS[i], counts[i]));
        }
        histogramChart.getData().setAll(series);
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }

    private static String formatScore(Double score) {
        if (score == null) {
            return "—";
        }
        if (score == Math.rint(score)) {
            return String.valueOf(score.intValue());
        }
        return String.valueOf(score);
    }

    private static String formatSubmitted(ExamAnswer answer) {
        if (answer == null || answer.getSubmittedAt() == null) {
            return "";
        }
        return SUBMITTED_FORMAT.format(answer.getSubmittedAt());
    }
}
