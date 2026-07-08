package com.hsts.client.gui;

import com.hsts.client.controller.ApprovalClientController;
import com.hsts.client.controller.BotChatClientController;
import com.hsts.client.controller.BotStatsClientController;
import com.hsts.client.controller.ExamBuilderClientController;
import com.hsts.client.controller.ExamTakingClientController;
import com.hsts.client.controller.ExamTimeClientController;
import com.hsts.client.controller.GradingClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.controller.QuestionClientController;
import com.hsts.client.controller.ResultsClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Student;
import com.hsts.shared.model.SubjectCoordinator;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Landing screen after login (SUC-9). Shows only the actions relevant to
 * the logged-in user's role, then hands off to the corresponding screen -
 * same pattern LoginWindow already used for QuestionManagementWindow.
 */
public class DashboardWindow {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label errorLabel;
    @FXML private Button questionsButton;
    @FXML private Button buildExamButton;
    @FXML private Button gradeExamsButton;
    @FXML private Button approveExamsButton;
    @FXML private Button takeExamButton;
    @FXML private Button myResultsButton;
    @FXML private Button extendTimeButton;
    @FXML private Button botStatsButton;
    @FXML private Button botChatButton;

    private User user;
    private ServerConnection client;
    private LoginClientController loginController;

    public void init(User user, ServerConnection client, LoginClientController loginController) {
        this.user = user;
        this.client = client;
        this.loginController = loginController;

        welcomeLabel.setText("Welcome, " + user.getFullName());
        roleLabel.setText("Role: " + user.getRole());

        boolean isCoordinator = user instanceof SubjectCoordinator;
        boolean isTeacher = user instanceof Teacher;
        boolean isStudent = user instanceof Student;

        questionsButton.setVisible(isTeacher);
        questionsButton.setManaged(isTeacher);
        buildExamButton.setVisible(isTeacher);
        buildExamButton.setManaged(isTeacher);
        gradeExamsButton.setVisible(isTeacher);
        gradeExamsButton.setManaged(isTeacher);
        approveExamsButton.setVisible(isCoordinator);
        approveExamsButton.setManaged(isCoordinator);
        takeExamButton.setVisible(isStudent);
        takeExamButton.setManaged(isStudent);
        myResultsButton.setVisible(isStudent);
        myResultsButton.setManaged(isStudent);
        extendTimeButton.setVisible(isTeacher);
        extendTimeButton.setManaged(isTeacher);
        botStatsButton.setVisible(isTeacher);
        botStatsButton.setManaged(isTeacher);
        botChatButton.setVisible(isStudent);
        botChatButton.setManaged(isStudent);
    }

    @FXML
    void handleQuestions(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/question_management.fxml"));
            Parent root = loader.load();
            QuestionManagementWindow qmw = loader.getController();
            QuestionClientController controller = new QuestionClientController(client);
            if (user instanceof Teacher teacher) {
                controller.setCurrentTeacher(teacher);
            }
            qmw.setController(controller);
            qmw.setLoggedInUser(user);
            switchScene(root, "HSTS - Question Bank");
        } catch (IOException e) {
            showError("Could not open question bank screen.");
        }
    }

    @FXML
    void handleBuildExam(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/exam_builder.fxml"));
            Parent root = loader.load();
            ExamBuilderWindow ebw = loader.getController();
            ExamBuilderClientController controller = new ExamBuilderClientController(client);
            controller.setCurrentTeacher((Teacher) user);
            ebw.init(controller, (Teacher) user);
            switchScene(root, "HSTS - Build Exam");
        } catch (IOException e) {
            showError("Could not open exam builder screen.");
        }
    }

    @FXML
    void handleGradeExams(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/grading.fxml"));
            Parent root = loader.load();
            GradingWindow gw = loader.getController();
            GradingClientController controller = new GradingClientController(client);
            gw.init(controller, (Teacher) user);
            switchScene(root, "HSTS - Grade Exams");
        } catch (IOException e) {
            showError("Could not open grading screen.");
        }
    }

    @FXML
    void handleApproveExams(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/approval.fxml"));
            Parent root = loader.load();
            ApprovalWindow aw = loader.getController();
            ApprovalClientController controller = new ApprovalClientController(client);
            aw.init(controller, (SubjectCoordinator) user);
            switchScene(root, "HSTS - Approve Exams");
        } catch (IOException e) {
            showError("Could not open approval screen.");
        }
    }

    @FXML
    void handleTakeExam(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/exam_taking.fxml"));
            Parent root = loader.load();
            ExamTakingWindow etw = loader.getController();
            ExamTakingClientController controller = new ExamTakingClientController(client);
            etw.init(controller, (Student) user);
            switchScene(root, "HSTS - Take Exam");
        } catch (IOException e) {
            showError("Could not open exam-taking screen.");
        }
    }

    @FXML
    void handleMyResults(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/results.fxml"));
            Parent root = loader.load();
            ResultsWindow rw = loader.getController();
            ResultsClientController controller = new ResultsClientController(client);
            rw.init(controller, (Student) user);
            switchScene(root, "HSTS - My Results");
        } catch (IOException e) {
            showError("Could not open results screen.");
        }
    }

    @FXML
    void handleExtendTime(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/exam_time.fxml"));
            Parent root = loader.load();
            ExamTimeWindow etw = loader.getController();
            ExamTimeClientController controller = new ExamTimeClientController(client);
            etw.init(controller, (Teacher) user);
            switchScene(root, "HSTS - Extend Exam Time");
        } catch (IOException e) {
            showError("Could not open extend-time screen.");
        }
    }

    @FXML
    void handleBotStats(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/bot_stats.fxml"));
            Parent root = loader.load();
            BotStatsWindow bsw = loader.getController();
            BotStatsClientController controller = new BotStatsClientController(client);
            bsw.init(controller, (Teacher) user);
            switchScene(root, "HSTS - Bot Usage Stats");
        } catch (IOException e) {
            showError("Could not open bot stats screen.");
        }
    }

    @FXML
    void handleBotChat(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/bot_chat.fxml"));
            Parent root = loader.load();
            BotChatWindow bcw = loader.getController();
            BotChatClientController controller = new BotChatClientController(client);
            bcw.init(controller, (Student) user);
            switchScene(root, "HSTS - Study Bot");
        } catch (IOException e) {
            showError("Could not open study bot screen.");
        }
    }

    private void switchScene(Parent root, String title) {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}
