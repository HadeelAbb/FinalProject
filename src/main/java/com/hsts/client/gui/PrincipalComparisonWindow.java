package com.hsts.client.gui;

import com.hsts.client.controller.LoginClientController;
import com.hsts.client.controller.PrincipalComparisonClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.GradeHistogramCalculator;
import com.hsts.shared.model.Principal;
import com.hsts.shared.model.PrincipalComparisonReport;
import com.hsts.shared.model.PrincipalComparisonRow;
import com.hsts.shared.model.PrincipalReportType;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.TreeSet;

public class PrincipalComparisonWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ComboBox<PrincipalReportType> reportTypeCombo;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Button generateButton;
    @FXML private Button refreshButton;
    @FXML private TableView<PrincipalComparisonRow> comparisonTable;
    @FXML private BarChart<String, Number> comparisonChart;
    @FXML private Label distributionLabel;
    @FXML private BarChart<String, Number> distributionChart;
    @FXML private Label errorLabel;

    private PrincipalComparisonClientController controller;
    private Principal navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;
    private List<Exam> exams = List.of();
    private List<ExamAnswer> confirmedResults = List.of();

    public void init(PrincipalComparisonClientController controller, Principal principal,
                     ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = principal;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setView(this);

        reportTypeCombo.getItems().setAll(PrincipalReportType.values());
        reportTypeCombo.getSelectionModel().select(PrincipalReportType.TEACHER);
        reportTypeCombo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldType, newType) -> refreshFilterValues());

        comparisonTable.setPlaceholder(new Label("Generate a report to compare exams."));
        comparisonTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldRow, newRow) -> updateDistribution(newRow));
        comparisonChart.setAnimated(false);
        distributionChart.setAnimated(false);
        setupTableColumns(PrincipalReportType.TEACHER);
        updateDistribution(null);

        controller.loadSelectors();
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
    void handleGenerate(ActionEvent event) {
        errorLabel.setText("");
        PrincipalReportType type = reportTypeCombo.getValue();
        String filter = filterCombo.getValue();
        if (type == null) {
            showError("Select a comparison type.");
            return;
        }
        if (filter == null || filter.isBlank()) {
            showError("Select a teacher, course, or student.");
            return;
        }
        setupTableColumns(type);
        controller.generateReport(type, filter);
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        errorLabel.setText("");
        controller.loadSelectors();
        PrincipalReportType type = reportTypeCombo.getValue();
        String filter = filterCombo.getValue();
        if (type != null && filter != null && !filter.isBlank()) {
            controller.generateReport(type, filter);
        }
    }

    public void displayExams(List<Exam> loaded) {
        exams = loaded != null ? loaded : List.of();
        errorLabel.setText("");
        refreshFilterValues();
    }

    public void displayConfirmedResults(List<ExamAnswer> loaded) {
        confirmedResults = loaded != null ? loaded : List.of();
        errorLabel.setText("");
        refreshFilterValues();
    }

    public void displayReport(PrincipalComparisonReport report) {
        List<PrincipalComparisonRow> rows = report != null && report.getRows() != null
                ? report.getRows() : List.of();
        setupTableColumns(report != null ? report.getReportType() : reportTypeCombo.getValue());
        comparisonTable.getItems().setAll(rows);
        updateComparisonChart(rows);
        if (!rows.isEmpty()) {
            comparisonTable.getSelectionModel().select(0);
        } else {
            updateDistribution(null);
            comparisonTable.setPlaceholder(new Label("No exams match this comparison."));
        }
        errorLabel.setText("");
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }

    private void refreshFilterValues() {
        PrincipalReportType type = reportTypeCombo.getValue();
        String previous = filterCombo.getValue();
        TreeSet<String> values = new TreeSet<>();
        if (type == PrincipalReportType.TEACHER) {
            for (Exam exam : exams) {
                if (exam.getCreatedByTeacherId() != null && !exam.getCreatedByTeacherId().isBlank()) {
                    values.add(exam.getCreatedByTeacherId());
                }
            }
        } else if (type == PrincipalReportType.COURSE) {
            for (Exam exam : exams) {
                if (exam.getCourseId() != null && !exam.getCourseId().isBlank()) {
                    values.add(exam.getCourseId());
                }
            }
        } else if (type == PrincipalReportType.STUDENT) {
            for (ExamAnswer answer : confirmedResults) {
                if (answer.getStudentId() != null && !answer.getStudentId().isBlank()) {
                    values.add(answer.getStudentId());
                }
            }
        }
        filterCombo.getItems().setAll(values);
        if (previous != null && values.contains(previous)) {
            filterCombo.getSelectionModel().select(previous);
        } else if (!values.isEmpty()) {
            filterCombo.getSelectionModel().select(values.first());
        } else {
            filterCombo.getSelectionModel().clearSelection();
        }
    }

    private void setupTableColumns(PrincipalReportType type) {
        PrincipalReportType mode = type != null ? type : PrincipalReportType.TEACHER;
        TableColumn<PrincipalComparisonRow, String> examCol = column("Exam", 160,
                row -> {
                    String id = row.getExamId() != null ? row.getExamId() : "";
                    String title = row.getExamTitle() != null ? row.getExamTitle() : "";
                    return title.isBlank() ? id : id + " " + title;
                });
        TableColumn<PrincipalComparisonRow, String> courseCol = column("Course", 90,
                row -> row.getCourseId());
        TableColumn<PrincipalComparisonRow, String> teacherCol = column("Teacher", 100,
                row -> row.getTeacherId());
        TableColumn<PrincipalComparisonRow, String> resultsCol = column("Confirmed", 90,
                row -> String.valueOf(row.getConfirmedCount()));
        TableColumn<PrincipalComparisonRow, String> meanCol = column("Mean", 70,
                row -> formatScore(row.getMean()));
        TableColumn<PrincipalComparisonRow, String> medianCol = column("Median", 70,
                row -> formatScore(row.getMedian()));
        TableColumn<PrincipalComparisonRow, String> studentCol = column("Student Grade", 110,
                row -> formatScore(row.getStudentGrade()));

        if (mode == PrincipalReportType.STUDENT) {
            comparisonTable.getColumns().setAll(examCol, courseCol, teacherCol, studentCol, meanCol, medianCol);
        } else if (mode == PrincipalReportType.COURSE) {
            comparisonTable.getColumns().setAll(examCol, teacherCol, resultsCol, meanCol, medianCol);
        } else {
            comparisonTable.getColumns().setAll(examCol, courseCol, resultsCol, meanCol, medianCol);
        }
        comparisonTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private TableColumn<PrincipalComparisonRow, String> column(String title, double width,
                                                               java.util.function.Function<PrincipalComparisonRow, String> value) {
        TableColumn<PrincipalComparisonRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() != null ? emptyToDash(value.apply(c.getValue())) : ""));
        column.setPrefWidth(width);
        return column;
    }

    private void updateComparisonChart(List<PrincipalComparisonRow> rows) {
        XYChart.Series<String, Number> meanSeries = new XYChart.Series<>();
        meanSeries.setName("Mean");
        XYChart.Series<String, Number> medianSeries = new XYChart.Series<>();
        medianSeries.setName("Median");
        for (PrincipalComparisonRow row : rows) {
            if (row.getMean() == null || row.getMedian() == null) {
                continue;
            }
            String label = row.getExamId() != null ? row.getExamId() : "";
            meanSeries.getData().add(new XYChart.Data<>(label, row.getMean()));
            medianSeries.getData().add(new XYChart.Data<>(label, row.getMedian()));
        }
        comparisonChart.getData().setAll(meanSeries, medianSeries);
    }

    private void updateDistribution(PrincipalComparisonRow row) {
        int[] counts = row != null && row.getDeciles() != null
                ? row.getDeciles() : new int[GradeHistogramCalculator.BUCKET_LABELS.length];
        if (row != null && row.getExamId() != null) {
            distributionLabel.setText("Selected Exam Distribution (" + row.getExamId() + ")");
        } else {
            distributionLabel.setText("Selected Exam Distribution");
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Students");
        int length = Math.min(counts.length, GradeHistogramCalculator.BUCKET_LABELS.length);
        for (int i = 0; i < GradeHistogramCalculator.BUCKET_LABELS.length; i++) {
            int count = i < length ? counts[i] : 0;
            series.getData().add(new XYChart.Data<>(GradeHistogramCalculator.BUCKET_LABELS[i], count));
        }
        distributionChart.getData().setAll(series);
    }

    private static String formatScore(Double score) {
        if (score == null) {
            return "—";
        }
        if (score == Math.rint(score)) {
            return String.valueOf(score.intValue());
        }
        return String.format("%.2f", score);
    }

    private static String emptyToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
