package com.hsts.client.gui;

import com.hsts.client.controller.ApprovalClientController;
import com.hsts.client.controller.BotChatClientController;
import com.hsts.client.controller.BotStatsClientController;
import com.hsts.client.controller.ExamBuilderClientController;
import com.hsts.client.controller.ExamTakingClientController;
import com.hsts.client.controller.ExamTimeClientController;
import com.hsts.client.controller.GradingClientController;
import com.hsts.client.controller.LoginClientController;
import com.hsts.client.controller.PrincipalClientController;
import com.hsts.client.controller.PrincipalComparisonClientController;
import com.hsts.client.controller.PrincipalQuestionBankClientController;
import com.hsts.client.controller.QuestionClientController;
import com.hsts.client.controller.ResultsClientController;
import com.hsts.client.controller.TeacherResultsClientController;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Principal;
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

public class DashboardWindow {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.layout.VBox questionBankSection;
    @FXML private javafx.scene.layout.VBox examsSection;
    @FXML private javafx.scene.layout.VBox studyBotSection;
    @FXML private javafx.scene.layout.VBox principalSection;
    @FXML private Button questionsButton;
    @FXML private Button buildExamButton;
    @FXML private Button gradeExamsButton;
    @FXML private Button examResultsButton;
    @FXML private Button approveExamsButton;
    @FXML private Button takeExamButton;
    @FXML private Button myResultsButton;
    @FXML private Button extendTimeButton;
    @FXML private Button botStatsButton;
    @FXML private Button botChatButton;
    @FXML private Button principalOverviewButton;
    @FXML private Button principalComparisonButton;
    @FXML private Button principalQuestionBankButton;

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
        boolean isTeacher = user instanceof Teacher && !(user instanceof SubjectCoordinator);
        boolean isStudent = user instanceof Student;
        boolean isPrincipal = user instanceof Principal;

        questionsButton.setVisible(isTeacher);
        questionsButton.setManaged(isTeacher);
        buildExamButton.setVisible(isTeacher);
        buildExamButton.setManaged(isTeacher);
        gradeExamsButton.setVisible(isTeacher);
        gradeExamsButton.setManaged(isTeacher);
        examResultsButton.setVisible(isTeacher);
        examResultsButton.setManaged(isTeacher);
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
        principalOverviewButton.setVisible(isPrincipal);
        principalOverviewButton.setManaged(isPrincipal);
        principalComparisonButton.setVisible(isPrincipal);
        principalComparisonButton.setManaged(isPrincipal);
        principalQuestionBankButton.setVisible(isPrincipal);
        principalQuestionBankButton.setManaged(isPrincipal);

        // Hide the whole section (header + separator too) when nothing inside it
        // would be visible for this role - otherwise an empty title/line shows
        // even though every button underneath it is hidden.
        boolean showQuestionBankSection = isTeacher;
        boolean showExamsSection = isTeacher || isCoordinator || isStudent;
        boolean showStudyBotSection = isTeacher || isStudent;
        boolean showPrincipalSection = isPrincipal;

        questionBankSection.setVisible(showQuestionBankSection);
        questionBankSection.setManaged(showQuestionBankSection);
        examsSection.setVisible(showExamsSection);
        examsSection.setManaged(showExamsSection);
        studyBotSection.setVisible(showStudyBotSection);
        studyBotSection.setManaged(showStudyBotSection);
        principalSection.setVisible(showPrincipalSection);
        principalSection.setManaged(showPrincipalSection);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        NavigationHelper.logoutWithConfirmation(welcomeLabel, client, loginController);
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
            qmw.setNavigation(user, client, loginController);
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
            ebw.init(controller, (Teacher) user, client, loginController);
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
            gw.init(controller, (Teacher) user, client, loginController);
            switchScene(root, "HSTS - Grade Exams");
        } catch (IOException e) {
            showError("Could not open grading screen.");
        }
    }

    @FXML
    void handleExamResults(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/teacher_results.fxml"));
            Parent root = loader.load();
            TeacherResultsWindow trw = loader.getController();
            TeacherResultsClientController controller = new TeacherResultsClientController(client);
            trw.init(controller, (Teacher) user, client, loginController);
            switchScene(root, "HSTS - Exam Results");
        } catch (IOException e) {
            showError("Could not open exam results screen.");
        }
    }

    @FXML
    void handleApproveExams(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/approval.fxml"));
            Parent root = loader.load();
            ApprovalWindow aw = loader.getController();
            ApprovalClientController controller = new ApprovalClientController(client);
            aw.init(controller, (SubjectCoordinator) user, client, loginController);
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
            etw.init(controller, (Student) user, client, loginController);
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
            rw.init(controller, (Student) user, client, loginController);
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
            etw.init(controller, (Teacher) user, client, loginController);
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
            bsw.init(controller, (Teacher) user, client, loginController);
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
            bcw.init(controller, (Student) user, client, loginController);
            switchScene(root, "HSTS - Study Bot");
        } catch (IOException e) {
            showError("Could not open study bot screen.");
        }
    }

    @FXML
    void handlePrincipalOverview(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/principal_overview.fxml"));
            Parent root = loader.load();
            PrincipalOverviewWindow pow = loader.getController();
            PrincipalClientController controller = new PrincipalClientController(client);
            pow.init(controller, (Principal) user, client, loginController);
            switchScene(root, "HSTS - Principal Overview");
        } catch (IOException e) {
            showError("Could not open principal overview screen.");
        }
    }

    @FXML
    void handlePrincipalComparison(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/principal_comparison.fxml"));
            Parent root = loader.load();
            PrincipalComparisonWindow pcw = loader.getController();
            PrincipalComparisonClientController controller = new PrincipalComparisonClientController(client);
            pcw.init(controller, (Principal) user, client, loginController);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("HSTS - Comparison Reports");
            stage.setScene(new Scene(root, 1000, 760));
        } catch (IOException e) {
            showError("Could not open comparison reports screen.");
        }
    }

    @FXML
    void handlePrincipalQuestionBank(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hsts/client/gui/principal_question_bank.fxml"));
            Parent root = loader.load();
            PrincipalQuestionBankWindow pqbw = loader.getController();
            PrincipalQuestionBankClientController controller = new PrincipalQuestionBankClientController(client);
            pqbw.init(controller, (Principal) user, client, loginController);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("HSTS - Question Bank");
            stage.setScene(new Scene(root, 1000, 640));
        } catch (IOException e) {
            showError("Could not open question bank screen.");
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