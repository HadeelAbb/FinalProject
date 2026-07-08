package com.hsts.client.gui;

import com.hsts.client.controller.ExamTimeClientController;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamStatus;
import com.hsts.shared.model.Teacher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.util.List;

public class ExamTimeWindow {

    @FXML private ListView<Exam> examListView;
    @FXML private Spinner<Integer> minutesSpinner;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private ExamTimeClientController controller;

    public void init(ExamTimeClientController controller, Teacher teacher) {
        this.controller = controller;
        controller.setCurrentTeacher(teacher);
        controller.setView(this);

        minutesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 10, 1));
        controller.refreshMyExams();
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        controller.refreshMyExams();
    }

    @FXML
    void handleExtend(ActionEvent event) {
        Exam selected = examListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an exam first.");
            return;
        }
        if (selected.getStatus() != ExamStatus.APPROVED) {
            showError("Only approved exams can have their time extended.");
            return;
        }
        controller.extend(selected.getExamId(), minutesSpinner.getValue());
    }

    public void displayExams(List<Exam> exams) {
        examListView.getItems().setAll(exams.stream().filter(e -> e.getStatus() == ExamStatus.APPROVED).toList());
        errorLabel.setText("");
    }

    public void onExtended(Exam exam) {
        statusLabel.setText(exam.getTitle() + " extended - new duration: " + exam.getDurationMinutes() + " min.");
    }

    public void showError(String message) {
        errorLabel.setText(message);
        statusLabel.setText("");
    }
}
