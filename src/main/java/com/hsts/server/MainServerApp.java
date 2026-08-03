package com.hsts.server;

import com.hsts.server.network.ConnectionRegistry;
import com.hsts.server.network.HSTSServer;
import com.hsts.server.network.ServerRequestRouter;
import com.hsts.shared.model.BotInteraction;
import com.hsts.shared.model.BotUsageStats;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.model.User;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.CreateExamAutoData;
import com.hsts.shared.net.dto.CreateExamManualData;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.DeleteQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;
import com.hsts.shared.net.dto.ExamApprovalDecisionData;
import com.hsts.shared.net.dto.ExtendExamTimeData;
import com.hsts.shared.net.dto.GetAvailableExamsData;
import com.hsts.shared.net.dto.GetExamAnswerCopyData;
import com.hsts.shared.net.dto.GetExamDetailData;
import com.hsts.shared.net.dto.GetMyExamsData;
import com.hsts.shared.net.dto.GetMyResultsData;
import com.hsts.shared.net.dto.GetPendingGradingData;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.LogoutData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import com.hsts.shared.net.dto.StartExamData;
import com.hsts.shared.net.dto.SubmitExamData;
import com.hsts.shared.net.dto.SubmitExamForApprovalData;
import ocsf.server.ConnectionToClient;
import server.controllers.ExamServerController;
import server.controllers.LoginServerController;
import server.controllers.QuestionServerController;
import server.db.DatabaseManager;
import server.db.repository.BotRepositoryImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Production Central Entry Point for the HSTS Backend.
 *
 * This version keeps the working P2 networking/session infrastructure:
 * - ConnectionRegistry
 * - targeted user connection tracking
 * - LOGOUT cleanup
 * - disconnect cleanup
 *
 * It also restores the old exam and bot routes from the previous MainServerApp.
 */
public class MainServerApp {

    private static final int PORT = 3000;
    private static final String DEFAULT_COURSE_ID = "11";

    public static void main(String[] args) {
        System.out.println("=== INITIALIZING PRODUCTION HSTS CENTRAL SERVER ===");

        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.connect()) {
            System.err.println(">>> CRITICAL ERROR: Unable to connect to MySQL database. Aborting server startup.");
            return;
        }
        System.out.println("[DATABASE] Hard drive connection established successfully.");

        LoginServerController loginServerController = new LoginServerController();
        QuestionServerController questionServerController = new QuestionServerController();
        ExamServerController examServerController = new ExamServerController();
        BotRepositoryImpl botRepository = new BotRepositoryImpl();

        ServerRequestRouter router = new ServerRequestRouter();
        HSTSServer server = new HSTSServer(PORT);
        ConnectionRegistry connectionRegistry = new ConnectionRegistry();

        server.setConnectionRegistry(connectionRegistry);

        registerAuthRoutes(router, loginServerController);
        registerQuestionRoutes(router, questionServerController, server);
        registerExamRoutes(router, examServerController);
        registerBotRoutes(router, botRepository);
        configureSessionHandling(server, router, loginServerController, connectionRegistry);

        startServer(server);
    }

    // =========================================================================
    // AUTH ROUTES
    // =========================================================================

    private static void registerAuthRoutes(ServerRequestRouter router,
                                           LoginServerController loginServerController) {
        router.registerHandler(Command.LOGIN, request -> {
            if (!(request.getPayload() instanceof LoginData data)) {
                return Response.failure(Command.LOGIN, "Invalid login payload.", request.getRequestId());
            }

            String username = data.getUsername();
            String password = data.getPassword();

            if (isBlank(username) || password == null) {
                return Response.failure(Command.LOGIN, "Invalid username or password", request.getRequestId());
            }

            if (loginServerController.isAlreadyLoggedIn(username)) {
                return Response.failure(
                        Command.LOGIN,
                        "User is already active in a session.",
                        request.getRequestId()
                );
            }

            boolean isAuthenticated = loginServerController.login(username, password);
            if (!isAuthenticated) {
                return Response.failure(Command.LOGIN, "Invalid username or password", request.getRequestId());
            }

            // FETCH DYNAMIC USER PROFILE FROM DATABASE BASED ON ROLE
            User authenticatedUser = buildAuthenticatedUserProfile(username);

            if (authenticatedUser == null) {
                return Response.failure(Command.LOGIN, "User profile not found in database.", request.getRequestId());
            }

            return Response.success(
                    Command.LOGIN,
                    authenticatedUser,
                    "Database authentication success.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.LOGOUT, request ->
                Response.success(Command.LOGOUT, null, "Logged out successfully.", request.getRequestId())
        );
    }

    // 🛠️ DYNAMIC USER FACTORY BASED ON MYSQL ROLE
    private static User buildAuthenticatedUserProfile(String username) {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return null;

        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Try reading role, falling back to TEACHER if column/data is missing
                    String role = "";
                    try {
                        role = rs.getString("role");
                    } catch (Exception e) {
                        role = "TEACHER";
                    }

                    // Try reading name details safely
                    String firstName = "User";
                    String lastName = username;
                    try {
                        if (rs.getString("first_name") != null) firstName = rs.getString("first_name");
                        if (rs.getString("last_name") != null) lastName = rs.getString("last_name");
                    } catch (Exception ignored) {}

                    if ("STUDENT".equalsIgnoreCase(role)) {
                        com.hsts.shared.model.Student student = new com.hsts.shared.model.Student();
                        student.setId(username);
                        student.setFirstName(firstName);
                        student.setLastName(lastName);
                        student.setRole("STUDENT");
                        return student;
                    } else if ("COORDINATOR".equalsIgnoreCase(role) || "SUBJECT_COORDINATOR".equalsIgnoreCase(role)) {
                        com.hsts.shared.model.SubjectCoordinator coord = new com.hsts.shared.model.SubjectCoordinator();
                        coord.setId(username);
                        coord.setFirstName(firstName);
                        coord.setLastName(lastName);
                        coord.setRole("SUBJECT_COORDINATOR");
                        return coord;
                    } else {
                        Teacher teacher = new Teacher();
                        teacher.setId(username);
                        teacher.setFirstName(firstName);
                        teacher.setLastName(lastName);
                        teacher.setRole("TEACHER");
                        teacher.setCourses(loadCoursesFromDatabase());
                        return teacher;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[SERVER-ERROR] Failed to fetch user profile: " + e.getMessage());
        }

        // Fallback if user is authenticated by LoginServerController but missing from SELECT *
        com.hsts.shared.model.Student fallbackStudent = new com.hsts.shared.model.Student();
        fallbackStudent.setId(username);
        fallbackStudent.setFirstName(username);
        fallbackStudent.setLastName("");
        fallbackStudent.setRole("STUDENT");
        return fallbackStudent;
    }

    private static Teacher buildAuthenticatedTeacherProfile(String username) {
        Teacher teacherProfile = new Teacher();
        teacherProfile.setId(username);
        teacherProfile.setFirstName("Authenticated");
        teacherProfile.setLastName("Teacher");
        teacherProfile.setRole("TEACHER");
        return teacherProfile;
    }

    private static List<Course> loadCoursesFromDatabase() {
        List<Course> courses = new ArrayList<>();
        Connection conn = DatabaseManager.getInstance().getConnection();

        if (conn == null) {
            System.err.println("[SERVER-ERROR] Cannot fetch courses: database connection is null.");
            return courses;
        }

        String sql = "SELECT course_id, name FROM courses";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("course_id");
                String name = rs.getString("name");
                courses.add(new Course(id, name));
            }
        } catch (SQLException e) {
            System.err.println("[SERVER-ERROR] Failed to fetch real courses from SQL:");
            e.printStackTrace();
        }

        return courses;
    }

    // =========================================================================
    // QUESTION ROUTES
    // =========================================================================

    private static void registerQuestionRoutes(ServerRequestRouter router,
                                               QuestionServerController questionServerController,
                                               HSTSServer server) {
        router.registerHandler(Command.SEARCH_QUESTIONS, request -> {
            SearchQuestionsData data = (SearchQuestionsData) request.getPayload();

            String courseFilter = data.getCourseId() != null ? data.getCourseId() : DEFAULT_COURSE_ID;
            List<shared.entities.Question> rawDbResults =
                    questionServerController.searchQuestions(data.getTopic(), courseFilter);

            List<Question> formattedGuiResults = new ArrayList<>();

            if (rawDbResults != null) {
                for (shared.entities.Question dbQ : rawDbResults) {
                    formattedGuiResults.add(mapDbQuestionToGuiQuestion(dbQ, courseFilter));
                }
            }

            return Response.success(
                    Command.SEARCH_QUESTIONS,
                    formattedGuiResults,
                    "Database query loaded successfully.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.CREATE_QUESTION, request -> {
            CreateQuestionData wrapperDto = (CreateQuestionData) request.getPayload();

            String text = wrapperDto.getText();
            String difficulty = wrapperDto.getDifficulty() != null
                    ? wrapperDto.getDifficulty().toString()
                    : Difficulty.MEDIUM.toString();
            String instructions = wrapperDto.getInstructions() != null ? wrapperDto.getInstructions() : "";
            String topic = wrapperDto.getTopic();
            String courseId = wrapperDto.getCourseId() != null ? wrapperDto.getCourseId() : DEFAULT_COURSE_ID;

            List<String> answersList = new ArrayList<>();
            List<Integer> correctFlagsList = new ArrayList<>();
            extractAnswers(wrapperDto.getAnswers(), answersList, correctFlagsList);

            String newQuestionId = questionServerController.createQuestion(
                    text,
                    difficulty,
                    instructions,
                    topic,
                    courseId,
                    answersList,
                    correctFlagsList
            );

            if (newQuestionId == null) {
                return Response.failure(
                        Command.CREATE_QUESTION,
                        "Database insertion rejected the record.",
                        request.getRequestId()
                );
            }

            Question savedQ = new Question();
            savedQ.setQuestionId(newQuestionId);
            savedQ.setText(text);
            savedQ.setInstructions(instructions);
            savedQ.setTopic(topic);
            savedQ.setCourseId(courseId);
            savedQ.setDifficulty(wrapperDto.getDifficulty());
            savedQ.setAnswers(wrapperDto.getAnswers());

            server.sendToAllClients(
                    Response.success(Command.QUESTIONS_CHANGED, null, "A question was created.", null)
            );

            return Response.success(
                    Command.CREATE_QUESTION,
                    savedQ,
                    "Question saved to MySQL database successfully!",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.EDIT_QUESTION, request -> {
            EditQuestionData wrapperDto = (EditQuestionData) request.getPayload();

            String questionId = wrapperDto.getQuestionId();
            String newText = wrapperDto.getText();
            String newInstruction = wrapperDto.getInstructions() != null ? wrapperDto.getInstructions() : "";

            List<String> updatedAnswers = new ArrayList<>();
            List<Integer> correctnessBits = new ArrayList<>();
            extractAnswers(wrapperDto.getAnswers(), updatedAnswers, correctnessBits);

            boolean isUpdated = questionServerController.editQuestion(
                    questionId,
                    newText,
                    newInstruction,
                    updatedAnswers,
                    correctnessBits
            );

            if (!isUpdated) {
                return Response.failure(
                        Command.EDIT_QUESTION,
                        "Database update rejected the change.",
                        request.getRequestId()
                );
            }

            Question updatedQ = new Question();
            updatedQ.setQuestionId(questionId);
            updatedQ.setText(newText);
            updatedQ.setInstructions(newInstruction);
            updatedQ.setTopic(wrapperDto.getTopic());
            updatedQ.setCourseId(questionId != null && questionId.length() >= 2
                    ? questionId.substring(0, 2)
                    : null);
            updatedQ.setDifficulty(wrapperDto.getDifficulty());
            updatedQ.setAnswers(wrapperDto.getAnswers());

            server.sendToAllClients(
                    Response.success(Command.QUESTIONS_CHANGED, null, "A question was edited.", null)
            );

            return Response.success(
                    Command.EDIT_QUESTION,
                    updatedQ,
                    "Question updated in MySQL successfully!",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.DELETE_QUESTION, request -> {
            DeleteQuestionData wrapperDto = (DeleteQuestionData) request.getPayload();

            boolean isDeleted = questionServerController.deleteQuestion(wrapperDto.getQuestionId());

            if (!isDeleted) {
                return Response.failure(
                        Command.DELETE_QUESTION,
                        "Deletion command rejected by backend transaction.",
                        request.getRequestId()
                );
            }

            server.sendToAllClients(
                    Response.success(Command.QUESTIONS_CHANGED, null, "A question was deleted.", null)
            );

            return Response.success(
                    Command.DELETE_QUESTION,
                    wrapperDto,
                    "Record dropped from database completely.",
                    request.getRequestId()
            );
        });
    }

    private static Question mapDbQuestionToGuiQuestion(shared.entities.Question dbQ, String courseFilter) {
        Question guiQ = new Question();

        guiQ.setQuestionId(dbQ.getQuestionId());
        guiQ.setText(dbQ.getText());
        guiQ.setTopic(dbQ.getTopic());
        guiQ.setDifficulty(parseDifficulty(dbQ.getDifficulty()));
        guiQ.setCourseId(courseFilter);
        guiQ.setInstructions(dbQ.getInstructions());

        List<QuestionAnswer> guiAnswers = new ArrayList<>();
        if (dbQ.getAnswers() != null) {
            List<String> texts = dbQ.getAnswers();
            List<Boolean> flags = dbQ.getCorrectFlags();

            for (int i = 0; i < texts.size(); i++) {
                QuestionAnswer guiAns = new QuestionAnswer();
                guiAns.setText(texts.get(i));
                boolean isCorrect = flags != null && i < flags.size() && Boolean.TRUE.equals(flags.get(i));
                guiAns.setCorrect(isCorrect);
                guiAnswers.add(guiAns);
            }
        }
        guiQ.setAnswers(guiAnswers);

        return guiQ;
    }

    private static Difficulty parseDifficulty(String difficulty) {
        try {
            return Difficulty.valueOf(difficulty);
        } catch (Exception ex) {
            return Difficulty.MEDIUM;
        }
    }

    private static void extractAnswers(List<QuestionAnswer> answers,
                                       List<String> answerTexts,
                                       List<Integer> correctFlags) {
        if (answers == null) {
            return;
        }

        for (QuestionAnswer ans : answers) {
            if (ans.getText() != null && !ans.getText().trim().isEmpty()) {
                answerTexts.add(ans.getText());
                correctFlags.add(ans.isCorrect() ? 1 : 0);
            }
        }
    }

    // =========================================================================
    // EXAM ROUTES restored from old MainServerApp
    // =========================================================================

    private static void registerExamRoutes(ServerRequestRouter router,
                                           ExamServerController examServerController) {
        router.registerHandler(Command.CREATE_EXAM_MANUAL, request -> {
            CreateExamManualData data = (CreateExamManualData) request.getPayload();
            Exam exam = examServerController.createManualExam(data);

            if (exam == null) {
                return Response.failure(
                        Command.CREATE_EXAM_MANUAL,
                        "Failed to create manual exam.",
                        request.getRequestId()
                );
            }
            return Response.success(
                    Command.CREATE_EXAM_MANUAL,
                    exam,
                    "Draft exam created successfully.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.CREATE_EXAM_AUTO, request -> {
            CreateExamAutoData data = (CreateExamAutoData) request.getPayload();
            Exam exam = examServerController.createAutoExam(data);

            if (exam == null) {
                return Response.failure(
                        Command.CREATE_EXAM_AUTO,
                        "Insufficient matching questions for criteria.",
                        request.getRequestId()
                );
            }
            return Response.success(
                    Command.CREATE_EXAM_AUTO,
                    exam,
                    "Auto-generated exam created successfully.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.APPROVE_EXAM, request -> {
            ExamApprovalDecisionData data = (ExamApprovalDecisionData) request.getPayload();
            Exam resultExam;

            boolean isApproved = data.getReason() == null || data.getReason().isBlank();

            if (isApproved) {
                resultExam = examServerController.approveExam(data.getExamId(), data.getCoordinatorId());
            } else {
                resultExam = examServerController.rejectExam(
                        data.getExamId(),
                        data.getCoordinatorId(),
                        data.getReason()
                );
            }

            if (resultExam == null) {
                return Response.failure(
                        Command.APPROVE_EXAM,
                        "Exam decision processing failed.",
                        request.getRequestId()
                );
            }

            return Response.success(
                    Command.APPROVE_EXAM,
                    resultExam,
                    "Exam decision recorded.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.SUBMIT_EXAM, request -> {
            SubmitExamData data = (SubmitExamData) request.getPayload();
            ExamAnswer answer = examServerController.submitExam(data);

            if (answer == null) {
                return Response.failure(
                        Command.SUBMIT_EXAM,
                        "Exam submission failed.",
                        request.getRequestId()
                );
            }
            return Response.success(
                    Command.SUBMIT_EXAM,
                    answer,
                    "Exam submitted and scored automatically.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.CONFIRM_GRADE, request -> {
            ConfirmGradeData data = (ConfirmGradeData) request.getPayload();
            boolean isSuccess = examServerController.confirmGrade(data);

            if (!isSuccess) {
                return Response.failure(
                        Command.CONFIRM_GRADE,
                        "Failed to confirm grade in database.",
                        request.getRequestId()
                );
            }
            return Response.success(
                    Command.CONFIRM_GRADE,
                    true,
                    "Grade confirmed successfully.",
                    request.getRequestId()
            );
        });
        // The client sends REJECT_EXAM as its own command (separate from
        // APPROVE_EXAM) - this was previously unregistered, so every
        // rejection from the Approval screen just failed with no handler.
        router.registerHandler(Command.REJECT_EXAM, request -> {
            ExamApprovalDecisionData data = (ExamApprovalDecisionData) request.getPayload();
            if (data.getReason() == null || data.getReason().isBlank()) {
                return Response.failure(Command.REJECT_EXAM, "A rejection reason is required.", request.getRequestId());
            }
            Exam exam = examServerController.rejectExam(data.getExamId(), data.getCoordinatorId(), data.getReason());
            if (exam == null) {
                return Response.failure(Command.REJECT_EXAM, "Exam rejection failed.", request.getRequestId());
            }
            return Response.success(Command.REJECT_EXAM, exam, "Exam rejected.", request.getRequestId());
        });

        router.registerHandler(Command.SUBMIT_EXAM_FOR_APPROVAL, request -> {
            SubmitExamForApprovalData data = (SubmitExamForApprovalData) request.getPayload();
            Exam exam = examServerController.submitForApproval(data.getExamId());
            if (exam == null) {
                return Response.failure(Command.SUBMIT_EXAM_FOR_APPROVAL,
                        "Only a draft or rejected exam can be submitted for approval.", request.getRequestId());
            }
            return Response.success(Command.SUBMIT_EXAM_FOR_APPROVAL, exam,
                    "Exam submitted for approval.", request.getRequestId());
        });

        router.registerHandler(Command.GET_MY_EXAMS, request -> {
            GetMyExamsData data = (GetMyExamsData) request.getPayload();
            List<Exam> mine = examServerController.getMyExams(data.getTeacherId());
            return Response.success(Command.GET_MY_EXAMS, mine, null, request.getRequestId());
        });

        router.registerHandler(Command.GET_PENDING_APPROVAL_EXAMS, request -> {
            List<Exam> pending = examServerController.getPendingApprovalExams();
            return Response.success(Command.GET_PENDING_APPROVAL_EXAMS, pending, null, request.getRequestId());
        });

        router.registerHandler(Command.GET_AVAILABLE_EXAMS, request -> {
            GetAvailableExamsData data = (GetAvailableExamsData) request.getPayload();
            List<Exam> available = examServerController.getAvailableExams(data.getStudentId());
            return Response.success(Command.GET_AVAILABLE_EXAMS, available, null, request.getRequestId());
        });

        router.registerHandler(Command.START_EXAM, request -> {
            StartExamData data = (StartExamData) request.getPayload();
            Exam exam = examServerController.startExam(data, data.getExecutionCode());
            if (exam == null) {
                return Response.failure(Command.START_EXAM,
                        "Could not start this exam - check the execution code, exam status, and whether you've already taken it.",
                        request.getRequestId());
            }
            return Response.success(Command.START_EXAM, exam, "Exam started.", request.getRequestId());
        });

        router.registerHandler(Command.GET_EXAM_DETAIL, request -> {
            GetExamDetailData data = (GetExamDetailData) request.getPayload();
            var opt = examServerController.getExamDetail(data.getExamId());
            if (opt.isEmpty()) {
                return Response.failure(Command.GET_EXAM_DETAIL, "Exam not found.", request.getRequestId());
            }
            return Response.success(Command.GET_EXAM_DETAIL, opt.get(), null, request.getRequestId());
        });

        router.registerHandler(Command.GET_PENDING_GRADING, request -> {
            GetPendingGradingData data = (GetPendingGradingData) request.getPayload();
            List<ExamAnswer> pending = examServerController.getPendingGrading(data.getTeacherId());
            return Response.success(Command.GET_PENDING_GRADING, pending, null, request.getRequestId());
        });

        router.registerHandler(Command.GET_MY_RESULTS, request -> {
            GetMyResultsData data = (GetMyResultsData) request.getPayload();
            List<ExamAnswer> mine = examServerController.getMyResults(data.getStudentId());
            return Response.success(Command.GET_MY_RESULTS, mine, null, request.getRequestId());
        });

        router.registerHandler(Command.GET_EXAM_ANSWER_COPY, request -> {
            GetExamAnswerCopyData data = (GetExamAnswerCopyData) request.getPayload();
            Object[] copy = examServerController.getExamAnswerCopy(data.getExamAnswerId(), data.getStudentId());
            if (copy == null) {
                return Response.failure(Command.GET_EXAM_ANSWER_COPY, "Graded exam not found.", request.getRequestId());
            }
            return Response.success(Command.GET_EXAM_ANSWER_COPY, copy, null, request.getRequestId());
        });

        router.registerHandler(Command.EXTEND_EXAM_TIME, request -> {
            ExtendExamTimeData data = (ExtendExamTimeData) request.getPayload();
            Exam exam = examServerController.extendExamTime(data.getExamId(), data.getTeacherId(), data.getAdditionalMinutes());
            if (exam == null) {
                return Response.failure(Command.EXTEND_EXAM_TIME,
                        "Could not extend time - check that you created this exam.", request.getRequestId());
            }
            return Response.success(Command.EXTEND_EXAM_TIME, exam,
                    "Time extended by " + data.getAdditionalMinutes() + " minutes.", request.getRequestId());
        });
    }

    // =========================================================================
    // BOT ROUTES restored from old MainServerApp
    // =========================================================================

    private static void registerBotRoutes(ServerRequestRouter router,
                                          BotRepositoryImpl botRepository) {
        router.registerHandler(Command.ASK_BOT_QUESTION, request -> {
            try {
                if (request.getPayload() instanceof BotInteraction incoming) {
                    if (isBlank(incoming.getInteractionId())) {
                        incoming.setInteractionId("BOT-" + UUID.randomUUID().toString().substring(0, 8));
                    }

                    if (isBlank(incoming.getAnswer())) {
                        incoming.setAnswer("Automated Assistant: I received your question regarding course "
                                + incoming.getCourseId()
                                + ". Please review course materials or consult your instructor!");
                    }

                    botRepository.save(incoming);

                    System.out.println("[BOT] Study Bot answered student "
                            + incoming.getStudentId()
                            + " successfully.");

                    return Response.success(
                            Command.ASK_BOT_QUESTION,
                            incoming,
                            "Bot interaction recorded.",
                            request.getRequestId()
                    );
                }

                return Response.failure(
                        Command.ASK_BOT_QUESTION,
                        "Invalid payload for ASK_BOT_QUESTION.",
                        request.getRequestId()
                );
            } catch (Exception e) {
                return Response.failure(
                        Command.ASK_BOT_QUESTION,
                        "Database error: " + e.getMessage(),
                        request.getRequestId()
                );
            }
        });

        router.registerHandler(Command.GET_BOT_HISTORY, request -> {
            try {
                if (request.getPayload() instanceof String studentId) {
                    List<BotInteraction> history = botRepository.findByStudentId(studentId);
                    return Response.success(
                            Command.GET_BOT_HISTORY,
                            history,
                            "Bot history fetched.",
                            request.getRequestId()
                    );
                }

                return Response.failure(
                        Command.GET_BOT_HISTORY,
                        "Student ID must be a String.",
                        request.getRequestId()
                );
            } catch (Exception e) {
                return Response.failure(
                        Command.GET_BOT_HISTORY,
                        "Database error: " + e.getMessage(),
                        request.getRequestId()
                );
            }
        });
        // NOTE: the schema has no teacher-course assignment table, so unlike
        // the mock (which scopes this to the requesting teacher's own
        // courses), this aggregates bot usage across every course that has
        // any activity at all. Anonymized either way - never shows which
        // student asked what, only per-course totals.
        router.registerHandler(Command.GET_BOT_USAGE_STATS, request -> {
            try {
                List<BotInteraction> all = botRepository.findAll();
                java.util.Map<String, java.util.List<BotInteraction>> byCourse = new java.util.LinkedHashMap<>();
                for (BotInteraction interaction : all) {
                    byCourse.computeIfAbsent(interaction.getCourseId(), k -> new java.util.ArrayList<>())
                            .add(interaction);
                }
                List<BotUsageStats> stats = new java.util.ArrayList<>();
                for (var entry : byCourse.entrySet()) {
                    java.util.Set<String> uniqueStudents = new java.util.HashSet<>();
                    for (BotInteraction i : entry.getValue()) {
                        uniqueStudents.add(i.getStudentId());
                    }
                    stats.add(new BotUsageStats(entry.getKey(), entry.getValue().size(), uniqueStudents.size()));
                }
                return Response.success(Command.GET_BOT_USAGE_STATS, stats, null, request.getRequestId());
            } catch (Exception e) {
                return Response.failure(Command.GET_BOT_USAGE_STATS, "Database error: " + e.getMessage(), request.getRequestId());
            }
        });
    }

    // =========================================================================
    // P2 SESSION / CONNECTION HANDLING
    // =========================================================================

    private static void configureSessionHandling(HSTSServer server,
                                                 ServerRequestRouter router,
                                                 LoginServerController loginServerController,
                                                 ConnectionRegistry connectionRegistry) {
        server.setUserDisconnectHandler(sessionKey -> {
            if (isBlank(sessionKey)) {
                return;
            }

            try {
                loginServerController.logout(sessionKey);
            } catch (Exception exception) {
                System.err.println("Failed to logout disconnected user "
                        + sessionKey
                        + ": "
                        + exception.getMessage());
            }
        });

        server.setRouter(router);

        server.setRequestHandler((request, client) -> {
            if (request.getCommand() == Command.LOGOUT) {
                String sessionKey = resolveLogoutSessionKey(client, request, connectionRegistry);

                if (!isBlank(sessionKey)) {
                    try {
                        loginServerController.logout(sessionKey);
                    } catch (Exception exception) {
                        System.err.println("Failed to logout user "
                                + sessionKey
                                + ": "
                                + exception.getMessage());
                    }
                }

                unregisterLogoutSession(client, sessionKey, connectionRegistry);
                return Response.success(Command.LOGOUT, null, "Logged out successfully.", request.getRequestId());
            }

            Response response = router.route(request);

            if (request.getCommand() == Command.LOGIN) {
                registerSuccessfulLoginConnection(request, response, client, connectionRegistry);
            }

            return response;
        });
    }

    private static void registerSuccessfulLoginConnection(Request request,
                                                          Response response,
                                                          ConnectionToClient client,
                                                          ConnectionRegistry connectionRegistry) {
        if (response == null || !response.isSuccess() || !(response.getPayload() instanceof User user)) {
            return;
        }

        String userId = user.getId();
        if (!isBlank(userId)) {
            connectionRegistry.register(userId, client);
            client.setInfo("userId", userId);
        }

        Object payload = request.getPayload();
        if (payload instanceof LoginData loginData) {
            String username = loginData.getUsername();
            if (!isBlank(username)) {
                client.setInfo("username", username);
            }
        }
    }

    private static String resolveLogoutSessionKey(ConnectionToClient client,
                                                  Request request,
                                                  ConnectionRegistry connectionRegistry) {
        if (client != null) {
            Object usernameInfo = client.getInfo("username");
            if (usernameInfo instanceof String username && !isBlank(username)) {
                return username;
            }
        }

        if (request != null && request.getPayload() instanceof LogoutData logoutData) {
            String logoutUsername = logoutData.getUsername();
            if (!isBlank(logoutUsername)) {
                return logoutUsername;
            }
        }

        if (connectionRegistry != null && client != null) {
            String registryUserId = connectionRegistry.getUserId(client);
            if (!isBlank(registryUserId)) {
                return registryUserId;
            }
        }

        if (client != null) {
            Object infoUserId = client.getInfo("userId");
            if (infoUserId instanceof String userId && !isBlank(userId)) {
                return userId;
            }
        }

        return null;
    }

    private static void unregisterLogoutSession(ConnectionToClient client,
                                                String sessionKey,
                                                ConnectionRegistry connectionRegistry) {
        if (connectionRegistry == null || client == null) {
            return;
        }

        String registryUserId = connectionRegistry.getUserId(client);
        if (!isBlank(registryUserId)) {
            connectionRegistry.unregisterByUserId(registryUserId);
        }

        if (!isBlank(sessionKey) && !sessionKey.equals(registryUserId)) {
            connectionRegistry.unregisterByUserId(sessionKey);
        }

        connectionRegistry.unregisterByConnection(client);
    }

    private static void startServer(HSTSServer server) {
        try {
            server.startServer();
            System.out.println(">>> PRODUCTION SYSTEM ENGINE LISTENING LIVE ON SOCKET PORT " + PORT + " <<<");
        } catch (Exception e) {
            System.err.println(">>> FATAL: OCSF initialization frame collapsed. <<<");
            e.printStackTrace();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}