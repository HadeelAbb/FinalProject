package com.hsts.client.gui;

import com.hsts.client.controller.ExamTimeClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamStatus;
import com.hsts.shared.model.Teacher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.util.List;

public class ExamTimeWindow {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private ListView<Exam> examListView;
    @FXML private Spinner<Integer> minutesSpinner;
    @FXML private Button extendButton;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private ExamTimeClientController controller;
    private Teacher navUser;
    private ServerConnection navClient;
    private LoginClientController navLoginController;

    public void init(ExamTimeClientController controller, Teacher teacher,
                      ServerConnection client, LoginClientController loginController) {
        this.controller = controller;
        this.navUser = teacher;
        this.navClient = client;
        this.navLoginController = loginController;
        controller.setCurrentTeacher(teacher);
        controller.setView(this);

        examListView.setPlaceholder(new Label("No approved exams to extend."));
        minutesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 10, 1));
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
        Exam selected = examListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an exam first.");
            return;
        }
        if (selected.getStatus() != ExamStatus.APPROVED) {
            showError("Only approved exams can have their time extended.");
            return;
        }
        int minutes = minutesSpinner.getValue();
        if (!NavigationHelper.confirm("Add " + minutes + " minutes to \"" + selected.getTitle()
                + "\"? This applies immediately to any student currently taking it.")) {
            return;
        }
        extendButton.setDisable(true);
        controller.extend(selected.getExamId(), minutes);
    }

    public void displayExams(List<Exam> exams) {
        examListView.getItems().setAll(exams.stream().filter(e -> e.getStatus() == ExamStatus.APPROVED).toList());
        errorLabel.setText("");
        extendButton.setDisable(false);
    }

    public void onExtended(Exam exam) {
        extendButton.setDisable(false);
        statusLabel.setText(exam.getTitle() + " extended - new duration: " + exam.getDurationMinutes() + " min.");
    }

    public void showError(String message) {
        extendButton.setDisable(false);
        errorLabel.setText(message);
        statusLabel.setText("");
    }
}
