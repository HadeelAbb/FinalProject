package com.hsts.server;

import com.hsts.server.network.ConnectionRegistry;
import com.hsts.server.network.EventBus;
import com.hsts.server.network.HSTSServer;
import com.hsts.server.network.ServerRequestRouter;
import com.hsts.shared.net.ExamEvent;
import com.hsts.shared.net.EventType;
import com.hsts.shared.model.BotInteraction;
import com.hsts.shared.model.BotUsageStats;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.ExamStats;
import com.hsts.shared.model.PrincipalComparisonReport;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.QuestionIllustration;
import com.hsts.shared.model.Principal;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.model.User;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.CreateExamAutoData;
import com.hsts.shared.net.dto.CreateExamManualData;
import com.hsts.shared.net.dto.CreateExamVersionData;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.DeleteQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;
import com.hsts.shared.net.dto.ExamApprovalDecisionData;
import com.hsts.shared.net.dto.ExtendExamTimeData;
import com.hsts.shared.net.dto.GetAvailableExamsData;
import com.hsts.shared.net.dto.GetExamAnswerCopyData;
import com.hsts.shared.net.dto.GetExamDetailData;
import com.hsts.shared.net.dto.GetExamResultsData;
import com.hsts.shared.net.dto.GetExamStatsData;
import com.hsts.shared.net.dto.GetMyExamsData;
import com.hsts.shared.net.dto.GetMyResultsData;
import com.hsts.shared.net.dto.GetPendingGradingData;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.LogoutData;
import com.hsts.shared.net.dto.PrincipalComparisonReportData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import com.hsts.shared.net.dto.StartExamData;
import com.hsts.shared.net.dto.SubmitExamData;
import com.hsts.shared.net.dto.SubmitExamForApprovalData;
import ocsf.server.ConnectionToClient;
import server.controllers.ActiveExamTracker;
import server.controllers.AuthenticatedSession;
import server.controllers.ExamResultsAccess;
import server.controllers.ExamServerController;
import server.controllers.LoginServerController;
import server.controllers.QuestionCreateValidator;
import server.controllers.QuestionServerController;
import server.controllers.RequestAuthorizer;
import server.controllers.RequestIdentityBinder;
import server.db.DatabaseManager;
import server.db.repository.BotRepositoryImpl;
import com.hsts.shared.model.CourseBotConfig;
import com.hsts.shared.net.dto.GetBotConfigData;
import com.hsts.shared.net.dto.UpdateBotConfigData;
import server.db.repository.BotConfigRepositoryImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MainServerApp {

    private static final int PORT = 3000;
    private static final String DEFAULT_COURSE_ID = "11";
    private static final server.db.repository.BotConfigRepositoryImpl botConfigRepository =
            new server.db.repository.BotConfigRepositoryImpl();

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
        EventBus eventBus = new EventBus(server);

        server.setConnectionRegistry(connectionRegistry);

        // A server stop (IntelliJ's Stop button, Ctrl+C, normal exit) should always
        // result in every account being logged out - otherwise a session can get
        // stuck as "active" with no way to log back in until the server restarts
        // (and even then, only if nothing else left it stuck).
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SHUTDOWN] Server is stopping - logging out all active sessions...");
            loginServerController.logoutAll();
        }));

        registerAuthRoutes(router, loginServerController);
        registerQuestionRoutes(router, questionServerController, server, examServerController);
        registerExamRoutes(router, examServerController, eventBus);
        registerBotRoutes(router, botRepository, examServerController);
        configureSessionHandling(server, router, loginServerController, connectionRegistry, examServerController);

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

                    // init.sql stores a single full_name column (not first_name/last_name).
                    // Split it for the existing User model so dashboards show the real name.
                    String[] nameParts = splitFullName(rs.getString("full_name"), username);
                    String firstName = nameParts[0];
                    String lastName = nameParts[1];

                    String email = null;
                    try {
                        email = rs.getString("email");
                    } catch (Exception ignored) {
                        // email is optional in the schema
                    }

                    if ("STUDENT".equalsIgnoreCase(role)) {
                        com.hsts.shared.model.Student student = new com.hsts.shared.model.Student();
                        student.setId(username);
                        student.setFirstName(firstName);
                        student.setLastName(lastName);
                        student.setEmail(email);
                        student.setRole("STUDENT");
                        student.setCourses(loadStudentCoursesFromDatabase(username));
                        return student;
                    } else if ("COORDINATOR".equalsIgnoreCase(role) || "SUBJECT_COORDINATOR".equalsIgnoreCase(role)) {
                        com.hsts.shared.model.SubjectCoordinator coord = new com.hsts.shared.model.SubjectCoordinator();
                        coord.setId(username);
                        coord.setFirstName(firstName);
                        coord.setLastName(lastName);
                        coord.setEmail(email);
                        coord.setRole("SUBJECT_COORDINATOR");
                        return coord;
                    } else if ("PRINCIPAL".equalsIgnoreCase(role)) {
                        Principal principal = new Principal();
                        principal.setId(username);
                        principal.setFirstName(firstName);
                        principal.setLastName(lastName);
                        principal.setEmail(email);
                        principal.setRole("PRINCIPAL");
                        return principal;
                    } else {
                        Teacher teacher = new Teacher();
                        teacher.setId(username);
                        teacher.setFirstName(firstName);
                        teacher.setLastName(lastName);
                        teacher.setEmail(email);
                        teacher.setRole("TEACHER");
                        teacher.setCourses(loadTeacherCoursesFromDatabase(username));
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

    /**
     * Maps DB {@code users.full_name} onto the shared User firstName/lastName fields.
     * Uses the first whitespace-separated token as first name and the remainder as last name.
     */
    private static String[] splitFullName(String fullName, String usernameFallback) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{usernameFallback, ""};
        }
        String trimmed = fullName.trim().replaceAll("\\s+", " ");
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[]{trimmed, ""};
        }
        return new String[]{trimmed.substring(0, space), trimmed.substring(space + 1)};
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

    // R04/R11: only the courses this specific teacher is actually assigned to teach -
    // used to populate their course dropdowns (exam builder, question bank, etc.) client-side.
    private static List<Course> loadTeacherCoursesFromDatabase(String teacherId) {
        List<Course> courses = new ArrayList<>();
        Connection conn = DatabaseManager.getInstance().getConnection();

        if (conn == null) {
            System.err.println("[SERVER-ERROR] Cannot fetch teacher courses: database connection is null.");
            return courses;
        }

        String sql = "SELECT c.course_id, c.name FROM courses c " +
                "JOIN teacher_courses tc ON c.course_id = tc.course_id " +
                "WHERE tc.teacher_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, teacherId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(new Course(rs.getString("course_id"), rs.getString("name")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SERVER-ERROR] Failed to fetch teacher courses from SQL:");
            e.printStackTrace();
        }

        return courses;
    }

    // SUC 6.2: only the courses this specific student is actually enrolled in -
    // used to populate their course dropdowns (bot chat, etc.) client-side.
    private static List<Course> loadStudentCoursesFromDatabase(String studentId) {
        List<Course> courses = new ArrayList<>();
        Connection conn = DatabaseManager.getInstance().getConnection();

        if (conn == null) {
            System.err.println("[SERVER-ERROR] Cannot fetch student courses: database connection is null.");
            return courses;
        }

        String sql = "SELECT c.course_id, c.name FROM courses c " +
                "JOIN student_courses sc ON c.course_id = sc.course_id " +
                "WHERE sc.student_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(new Course(rs.getString("course_id"), rs.getString("name")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SERVER-ERROR] Failed to fetch student courses from SQL:");
            e.printStackTrace();
        }

        return courses;
    }

    // =========================================================================
    // QUESTION ROUTES
    // =========================================================================

    private static void registerQuestionRoutes(ServerRequestRouter router,
                                               QuestionServerController questionServerController,
                                               HSTSServer server,
                                               ExamServerController examServerController) {
        router.registerHandler(Command.SEARCH_QUESTIONS, request -> {
            SearchQuestionsData data = (SearchQuestionsData) request.getPayload();

            String courseFilter = data.getCourseId() != null ? data.getCourseId() : DEFAULT_COURSE_ID;
            String difficulty = data.getDifficulty() != null ? data.getDifficulty().toString() : null;
            List<shared.entities.Question> rawDbResults =
                    questionServerController.searchQuestions(
                            data.getTopic(), courseFilter, difficulty, data.isLatestOnly());

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

        router.registerHandler(Command.GET_ALL_QUESTIONS, request -> {
            SearchQuestionsData data = request.getPayload() instanceof SearchQuestionsData search
                    ? search : new SearchQuestionsData();
            String courseFilter = data.getCourseId() != null && !data.getCourseId().isBlank()
                    ? data.getCourseId() : null;
            String difficulty = data.getDifficulty() != null ? data.getDifficulty().toString() : null;
            List<shared.entities.Question> rawDbResults =
                    questionServerController.searchQuestions(data.getTopic(), courseFilter, difficulty);

            List<Question> formattedGuiResults = new ArrayList<>();
            if (rawDbResults != null) {
                for (shared.entities.Question dbQ : rawDbResults) {
                    formattedGuiResults.add(mapDbQuestionToGuiQuestion(dbQ, dbQ.getCourseId()));
                }
            }
            return Response.success(
                    Command.GET_ALL_QUESTIONS,
                    formattedGuiResults,
                    null,
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

            // R04: teacher can only create questions for courses she teaches.
            // teacherId is the authenticated teacher after RequestIdentityBinder.
            if (!examServerController.isTeacherAssignedToCourse(wrapperDto.getTeacherId(), courseId)) {
                return Response.failure(
                        Command.CREATE_QUESTION,
                        "You don't teach this course, so you can't add questions to it.",
                        request.getRequestId()
                );
            }

            String validationError = QuestionCreateValidator.validate(wrapperDto);
            if (validationError != null) {
                return Response.failure(
                        Command.CREATE_QUESTION,
                        validationError,
                        request.getRequestId()
                );
            }

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
                    correctFlagsList,
                    wrapperDto.getImageData(),
                    wrapperDto.getImagePath()
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
            savedQ.setRootQuestionId(newQuestionId);
            savedQ.setVersionNumber(1);
            savedQ.setLatest(true);
            QuestionIllustration.apply(savedQ, wrapperDto.getImageData(), wrapperDto.getImagePath());

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
            String actualCourseId = questionServerController.findCourseId(questionId);
            if (actualCourseId == null
                    || !examServerController.isTeacherAssignedToCourse(wrapperDto.getTeacherId(), actualCourseId)) {
                return Response.failure(
                        Command.EDIT_QUESTION,
                        RequestAuthorizer.NOT_AUTHORIZED,
                        request.getRequestId()
                );
            }

            String newText = wrapperDto.getText();
            String newInstruction = wrapperDto.getInstructions() != null ? wrapperDto.getInstructions() : "";

            String validationError = QuestionCreateValidator.validate(wrapperDto);
            if (validationError != null) {
                return Response.failure(
                        Command.EDIT_QUESTION,
                        validationError,
                        request.getRequestId()
                );
            }

            List<String> updatedAnswers = new ArrayList<>();
            List<Integer> correctnessBits = new ArrayList<>();
            extractAnswers(wrapperDto.getAnswers(), updatedAnswers, correctnessBits);

            QuestionServerController.VersionEditResult editResult = questionServerController.createNextVersion(
                    questionId,
                    newText,
                    newInstruction,
                    wrapperDto.getDifficulty() != null ? wrapperDto.getDifficulty().toString() : null,
                    wrapperDto.getTopic(),
                    updatedAnswers,
                    correctnessBits,
                    wrapperDto.getImageData(),
                    wrapperDto.getImagePath()
            );

            if (!editResult.success) {
                return Response.failure(
                        Command.EDIT_QUESTION,
                        editResult.errorMessage != null ? editResult.errorMessage : "Database update rejected the change.",
                        request.getRequestId()
                );
            }

            Question updatedQ = new Question();
            updatedQ.setQuestionId(editResult.newQuestionId);
            updatedQ.setText(newText);
            updatedQ.setInstructions(newInstruction);
            updatedQ.setTopic(wrapperDto.getTopic());
            updatedQ.setCourseId(actualCourseId);
            updatedQ.setDifficulty(wrapperDto.getDifficulty());
            updatedQ.setAnswers(wrapperDto.getAnswers());
            updatedQ.setRootQuestionId(editResult.rootQuestionId);
            updatedQ.setVersionNumber(editResult.newVersionNumber);
            updatedQ.setLatest(true);
            QuestionIllustration.apply(updatedQ, editResult.imageData, editResult.imageFilename);

            server.sendToAllClients(
                    Response.success(Command.QUESTIONS_CHANGED, null, "A question was edited.", null)
            );

            return Response.success(
                    Command.EDIT_QUESTION,
                    updatedQ,
                    "Question updated. Version " + editResult.newVersionNumber
                            + " created; previous version was preserved.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.DELETE_QUESTION, request -> {
            DeleteQuestionData wrapperDto = (DeleteQuestionData) request.getPayload();

            String actualCourseId = questionServerController.findCourseId(wrapperDto.getQuestionId());
            if (actualCourseId == null
                    || !examServerController.isTeacherAssignedToCourse(wrapperDto.getTeacherId(), actualCourseId)) {
                return Response.failure(
                        Command.DELETE_QUESTION,
                        RequestAuthorizer.NOT_AUTHORIZED,
                        request.getRequestId()
                );
            }

            String deleteError = questionServerController.deleteQuestion(wrapperDto.getQuestionId());

            if (deleteError != null) {
                return Response.failure(
                        Command.DELETE_QUESTION,
                        deleteError,
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
        guiQ.setCourseId(dbQ.getCourseId() != null && !dbQ.getCourseId().isBlank()
                ? dbQ.getCourseId() : courseFilter);
        guiQ.setInstructions(dbQ.getInstructions());
        guiQ.setRootQuestionId(dbQ.getRootQuestionId());
        guiQ.setVersionNumber(dbQ.getVersionNumber());
        guiQ.setLatest(dbQ.isLatest());
        QuestionIllustration.apply(guiQ, dbQ.getImageData(), dbQ.getImageFilename());

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
                                           ExamServerController examServerController,
                                           EventBus eventBus) {
        router.registerHandler(Command.CREATE_EXAM_MANUAL, request -> {
            CreateExamManualData data = (CreateExamManualData) request.getPayload();
            ExamServerController.CreateExamResult result = examServerController.tryCreateManualExam(data);

            if (result.exam == null) {
                return Response.failure(
                        Command.CREATE_EXAM_MANUAL,
                        result.errorMessage != null ? result.errorMessage : "Failed to create manual exam.",
                        request.getRequestId()
                );
            }
            return Response.success(
                    Command.CREATE_EXAM_MANUAL,
                    result.exam,
                    "Draft exam created successfully.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.CREATE_EXAM_AUTO, request -> {
            CreateExamAutoData data = (CreateExamAutoData) request.getPayload();
            ExamServerController.CreateExamResult result = examServerController.tryCreateAutoExam(data);

            if (result.exam == null) {
                return Response.failure(
                        Command.CREATE_EXAM_AUTO,
                        result.errorMessage != null ? result.errorMessage : "Insufficient matching questions for criteria.",
                        request.getRequestId()
                );
            }
            return Response.success(
                    Command.CREATE_EXAM_AUTO,
                    result.exam,
                    "Auto-generated exam created successfully.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.CREATE_EXAM_VERSION, request -> {
            CreateExamVersionData data = (CreateExamVersionData) request.getPayload();
            ExamServerController.CreateExamResult result = examServerController.tryCreateExamVersion(data);

            if (result.exam == null) {
                return Response.failure(
                        Command.CREATE_EXAM_VERSION,
                        result.errorMessage != null ? result.errorMessage : "Failed to create exam version.",
                        request.getRequestId()
                );
            }
            return Response.success(
                    Command.CREATE_EXAM_VERSION,
                    result.exam,
                    "New exam version created as draft. Coordinator approval is required before execution.",
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

            EventType eventType = isApproved ? EventType.EXAM_APPROVED : EventType.EXAM_REJECTED;
            String eventMessage = isApproved
                    ? "Your exam \"" + resultExam.getTitle() + "\" was approved."
                    : "Your exam \"" + resultExam.getTitle() + "\" was rejected.";
            eventBus.publish(new ExamEvent(eventType, resultExam.getExamId(), resultExam.getCourseId(),
                    resultExam.getCreatedByTeacherId(), eventMessage));

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
            eventBus.publish(new ExamEvent(EventType.EXAM_ANSWER_SUBMITTED, answer.getExamId(),
                    null, null, "A new exam submission is waiting to be graded."));
            return Response.success(
                    Command.SUBMIT_EXAM,
                    answer,
                    "Exam submitted and scored automatically.",
                    request.getRequestId()
            );
        });

        router.registerHandler(Command.CONFIRM_GRADE, request -> {
            ConfirmGradeData data = (ConfirmGradeData) request.getPayload();
            String failureReason = examServerController.confirmGradeWithReason(data);

            if (failureReason != null) {
                return Response.failure(
                        Command.CONFIRM_GRADE,
                        failureReason,
                        request.getRequestId()
                );
            }
            ExamAnswer confirmed = examServerController.findExamAnswerById(data.getExamAnswerId()).orElse(null);
            if (confirmed == null) {
                return Response.failure(
                        Command.CONFIRM_GRADE,
                        "Grade was saved but could not be reloaded.",
                        request.getRequestId()
                );
            }
            eventBus.publish(new ExamEvent(EventType.EXAM_GRADED, data.getExamAnswerId(),
                    null, null, "A grade was confirmed."));
            return Response.success(
                    Command.CONFIRM_GRADE,
                    confirmed,
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
            eventBus.publish(new ExamEvent(EventType.EXAM_REJECTED, exam.getExamId(), exam.getCourseId(),
                    exam.getCreatedByTeacherId(), "Your exam \"" + exam.getTitle() + "\" was rejected."));
            return Response.success(Command.REJECT_EXAM, exam, "Exam rejected.", request.getRequestId());
        });

        router.registerHandler(Command.SUBMIT_EXAM_FOR_APPROVAL, request -> {
            SubmitExamForApprovalData data = (SubmitExamForApprovalData) request.getPayload();
            Exam exam = examServerController.submitForApproval(data.getExamId(), data.getTeacherId());
            if (exam == null) {
                return Response.failure(Command.SUBMIT_EXAM_FOR_APPROVAL,
                        "Could not submit this exam for approval. It must be your draft or rejected exam, and question points must total exactly 100.",
                        request.getRequestId());
            }
            eventBus.publish(new ExamEvent(EventType.EXAM_SUBMITTED_FOR_APPROVAL, exam.getExamId(),
                    exam.getCourseId(), null, "A new exam is waiting for approval: " + exam.getTitle()));
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

        router.registerHandler(Command.GET_EXAM_RESULTS, request -> {
            GetExamResultsData data = (GetExamResultsData) request.getPayload();
            List<ExamAnswer> results = examServerController.getExamResults(data.getExamId(), data.getTeacherId());
            if (results == null) {
                return Response.failure(Command.GET_EXAM_RESULTS,
                        ExamResultsAccess.DENIED, request.getRequestId());
            }
            return Response.success(Command.GET_EXAM_RESULTS, results, null, request.getRequestId());
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
            com.hsts.shared.model.ExamExecution execution =
                    examServerController.extendExecutionTime(
                            data.getExecutionId(), data.getTeacherId(), data.getAdditionalMinutes());
            if (execution == null) {
                return Response.failure(Command.EXTEND_EXAM_TIME,
                        "Could not extend time - execution not found.", request.getRequestId());
            }
            // Still push a live update too, for anyone already mid-exam right now -
            // they don't need to wait for their next request to see the extra time.
            ExamEvent event = new ExamEvent(EventType.EXAM_TIME_EXTENDED, execution.getExamId(),
                    null, null,
                    "Your teacher extended this exam's time by " + data.getAdditionalMinutes() + " minutes.");
            event.setExtraMinutes(data.getAdditionalMinutes());
            eventBus.publish(event);
            return Response.success(Command.EXTEND_EXAM_TIME, execution,
                    "Time extended by " + data.getAdditionalMinutes() + " minutes for this execution - "
                            + "applies to students currently taking it AND anyone starting from now on.",
                    request.getRequestId());
        });

        // SUC 7.3.1: Principal's read-only access - no filtering by teacher/course/status
        router.registerHandler(Command.GET_ALL_EXAMS, request -> {
            List<Exam> all = examServerController.getAllExams();
            return Response.success(Command.GET_ALL_EXAMS, all, null, request.getRequestId());
        });

        router.registerHandler(Command.GET_ALL_RESULTS, request -> {
            List<ExamAnswer> all = examServerController.getAllResults();
            return Response.success(Command.GET_ALL_RESULTS, all, null, request.getRequestId());
        });

        // SUC 5 / 7.2 / 7.3.2: mean/median/decile stats for one exam
        router.registerHandler(Command.GET_EXAM_STATS, request -> {
            GetExamStatsData data = (GetExamStatsData) request.getPayload();
            Optional<ExamStats> stats = examServerController.getExamStats(data.getExamId());
            if (stats.isEmpty()) {
                return Response.failure(Command.GET_EXAM_STATS,
                        "No confirmed grades yet for this exam - statistics aren't available until at least one grade is confirmed.",
                        request.getRequestId());
            }
            return Response.success(Command.GET_EXAM_STATS, stats.get(), null, request.getRequestId());
        });

        router.registerHandler(Command.GET_PRINCIPAL_COMPARISON_REPORT, request -> {
            PrincipalComparisonReportData data = (PrincipalComparisonReportData) request.getPayload();
            if (data == null || data.getReportType() == null) {
                return Response.failure(Command.GET_PRINCIPAL_COMPARISON_REPORT,
                        "Report type is required.", request.getRequestId());
            }
            if (data.getFilterValue() == null || data.getFilterValue().isBlank()) {
                return Response.failure(Command.GET_PRINCIPAL_COMPARISON_REPORT,
                        "Select a teacher, course, or student.", request.getRequestId());
            }
            PrincipalComparisonReport report = examServerController.getPrincipalComparisonReport(
                    data.getReportType(), data.getFilterValue());
            return Response.success(Command.GET_PRINCIPAL_COMPARISON_REPORT, report, null, request.getRequestId());
        });

        // SUC 2.2: a teacher opens another execution (sitting) of an already-approved exam
        router.registerHandler(Command.CREATE_EXAM_EXECUTION, request -> {
            com.hsts.shared.net.dto.CreateExamExecutionData data =
                    (com.hsts.shared.net.dto.CreateExamExecutionData) request.getPayload();
            ExamServerController.CreateExecutionResult result = examServerController.tryCreateExecution(
                    data.getExamId(), data.getTeacherId(), data.getScheduledStart(), data.getScheduledEnd(),
                    data.getExecutionCode());
            if (result.execution == null) {
                return Response.failure(Command.CREATE_EXAM_EXECUTION,
                        result.errorMessage != null ? result.errorMessage
                                : "Could not open a new execution.",
                        request.getRequestId());
            }
            com.hsts.shared.model.ExamExecution execution = result.execution;
            eventBus.publish(new ExamEvent(EventType.EXECUTION_CREATED, execution.getExamId(),
                    null, null, "A new execution was opened - code: " + execution.getExecutionCode()));
            return Response.success(Command.CREATE_EXAM_EXECUTION, execution,
                    "New execution opened - code: " + execution.getExecutionCode(), request.getRequestId());
        });

        // Section 4: every execution this exam has ever had
        router.registerHandler(Command.GET_EXAM_EXECUTIONS, request -> {
            com.hsts.shared.net.dto.GetExamExecutionsData data =
                    (com.hsts.shared.net.dto.GetExamExecutionsData) request.getPayload();
            List<com.hsts.shared.model.ExamExecution> executions = examServerController.getExecutionsForExam(data.getExamId());
            return Response.success(Command.GET_EXAM_EXECUTIONS, executions, null, request.getRequestId());
        });

        // Section 4: started/finished/timed-out counts for one execution
        router.registerHandler(Command.GET_EXECUTION_STATS, request -> {
            com.hsts.shared.net.dto.GetExecutionStatsData data =
                    (com.hsts.shared.net.dto.GetExecutionStatsData) request.getPayload();
            com.hsts.shared.model.ExecutionStats stats = examServerController.getExecutionStats(data.getExecutionId());
            return Response.success(Command.GET_EXECUTION_STATS, stats, null, request.getRequestId());
        });
    }

    // =========================================================================
    // BOT ROUTES restored from old MainServerApp
    // =========================================================================

    private static void registerBotRoutes(ServerRequestRouter router,
                                          BotRepositoryImpl botRepository,
                                          ExamServerController examServerController) {
        server.controllers.BotApiClient botApiClient = new server.controllers.BotApiClient();

        router.registerHandler(Command.ASK_BOT_QUESTION, request -> {
            try {
                if (request.getPayload() instanceof com.hsts.shared.net.dto.AskBotQuestionData data) {
                    // SUC 6.2: a student may only use the bot for a course they're enrolled in.
                    if (!examServerController.isStudentEnrolled(data.getStudentId(), data.getCourseId())) {
                        return Response.failure(Command.ASK_BOT_QUESTION,
                                "You're not registered for that course, so the study bot isn't available to you for it.",
                                request.getRequestId());
                    }

                    // Course-aware exam lock: refuse BEFORE the external LLM call.
                    if (examServerController.hasActiveExamInCourse(data.getStudentId(), data.getCourseId())) {
                        System.out.println("[EXAM-LOCK] blocked ASK_BOT for " + data.getStudentId()
                                + " course=" + data.getCourseId());
                        return Response.failure(Command.ASK_BOT_QUESTION,
                                ActiveExamTracker.BOT_UNAVAILABLE_MESSAGE,
                                request.getRequestId());
                    }

                    // SUC-13 / Req 13: Check course bot configuration and pull knowledge sources[cite: 101, 103, 104]
                    java.util.Optional<com.hsts.shared.model.CourseBotConfig> configOpt =
                            botConfigRepository.findByCourseId(data.getCourseId());
                    if (configOpt.isPresent() && !configOpt.get().isActive()) {
                        return Response.failure(Command.ASK_BOT_QUESTION,
                                "The Study Bot for this course is currently disabled by the teacher.",
                                request.getRequestId());
                    }

                    String knowledgeSources = configOpt.map(com.hsts.shared.model.CourseBotConfig::getKnowledgeSources).orElse(null);

                    String interactionId = "BOT-" + UUID.randomUUID().toString().substring(0, 8);

                    // SUC-14: Query LLM with question, course context, and teacher knowledge sources[cite: 74, 103]
                    String realAnswer = botApiClient.ask(data.getQuestion(), data.getCourseId(), knowledgeSources);
                    String finalAnswer = realAnswer != null ? realAnswer
                            : "Sorry, I couldn't come up with a good answer to that right now. "
                              + "Please try rephrasing your question, or check with your instructor.";

                    BotInteraction incoming = new BotInteraction(interactionId, data.getStudentId(),
                            data.getCourseId(), data.getQuestion(), finalAnswer);

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
        // SUC-13 / Req 13: Fetch Bot Configuration for a Course[cite: 101, 103]
        router.registerHandler(Command.GET_BOT_CONFIG, request -> {
            try {
                if (request.getPayload() instanceof com.hsts.shared.net.dto.GetBotConfigData data) {
                    java.util.Optional<com.hsts.shared.model.CourseBotConfig> configOpt =
                            botConfigRepository.findByCourseId(data.getCourseId());

                    com.hsts.shared.model.CourseBotConfig config = configOpt.orElseGet(() ->
                            new com.hsts.shared.model.CourseBotConfig(data.getCourseId(), data.getCourseId() + " Study Bot", "", true, null)
                    );

                    return Response.success(Command.GET_BOT_CONFIG, config, null, request.getRequestId());
                }

                return Response.failure(Command.GET_BOT_CONFIG, "Invalid payload for GET_BOT_CONFIG.", request.getRequestId());
            } catch (Exception e) {
                return Response.failure(Command.GET_BOT_CONFIG, "Database error: " + e.getMessage(), request.getRequestId());
            }
        });

// SUC-13 / Req 13: Update Bot Knowledge Sources and Active State[cite: 101, 103]
        router.registerHandler(Command.UPDATE_BOT_CONFIG, request -> {
            try {
                if (request.getPayload() instanceof com.hsts.shared.net.dto.UpdateBotConfigData data) {
                    if (!examServerController.isTeacherAssignedToCourse(data.getTeacherId(), data.getCourseId())) {
                        return Response.failure(Command.UPDATE_BOT_CONFIG,
                                "You do not teach course " + data.getCourseId() + ".", request.getRequestId());
                    }

                    if (data.getBotName() == null || data.getBotName().isBlank()) {
                        return Response.failure(Command.UPDATE_BOT_CONFIG, "Bot name is required.", request.getRequestId());
                    }

                    com.hsts.shared.model.CourseBotConfig config = new com.hsts.shared.model.CourseBotConfig(
                            data.getCourseId(),
                            data.getBotName().trim(),
                            data.getKnowledgeSources() != null ? data.getKnowledgeSources().trim() : "",
                            data.isActive(),
                            data.getTeacherId()
                    );

                    boolean ok = botConfigRepository.saveOrUpdate(config);
                    if (!ok) {
                        return Response.failure(Command.UPDATE_BOT_CONFIG, "Failed to save bot configuration.", request.getRequestId());
                    }

                    return Response.success(Command.UPDATE_BOT_CONFIG, config, "Bot configuration updated successfully.", request.getRequestId());
                }

                return Response.failure(Command.UPDATE_BOT_CONFIG, "Invalid payload for UPDATE_BOT_CONFIG.", request.getRequestId());
            } catch (Exception e) {
                return Response.failure(Command.UPDATE_BOT_CONFIG, "Database error: " + e.getMessage(), request.getRequestId());
            }
        });

        router.registerHandler(Command.GET_BOT_HISTORY, request -> {
            try {
                if (request.getPayload() instanceof com.hsts.shared.net.dto.GetBotHistoryData historyData) {
                    List<BotInteraction> history = botRepository.findByStudentId(historyData.getStudentId());
                    return Response.success(
                            Command.GET_BOT_HISTORY,
                            history,
                            "Bot history fetched.",
                            request.getRequestId()
                    );
                }

                return Response.failure(
                        Command.GET_BOT_HISTORY,
                        "Invalid payload for GET_BOT_HISTORY.",
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
                                                 ConnectionRegistry connectionRegistry,
                                                 ExamServerController examServerController) {
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
            // Active exam sittings are NOT cleared here. Disconnect must not
            // unlock Study Bot for a student who started and never submitted.
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
                    // Active exam sittings are NOT cleared here. Logout must not
                    // unlock Study Bot for a student who started and never submitted.
                }

                unregisterLogoutSession(client, sessionKey, connectionRegistry);
                return Response.success(Command.LOGOUT, null, "Logged out successfully.", request.getRequestId());
            }

            AuthenticatedSession session = connectionRegistry != null
                    ? connectionRegistry.getSession(client) : null;
            if (request.getCommand() != Command.LOGIN) {
                String authError = RequestAuthorizer.authorize(request.getCommand(), session);
                if (authError != null) {
                    return Response.failure(request.getCommand(), authError, request.getRequestId());
                }
                RequestIdentityBinder.bindActor(request, session);

                if (ExamResultsAccess.requiresExamAccessCheck(request.getCommand())) {
                    Exam exam;
                    if (ExamResultsAccess.requiresExecutionLookup(request.getCommand())) {
                        String executionId = ExamResultsAccess.executionIdFromPayload(request.getPayload());
                        var execution = examServerController.findExecutionById(executionId).orElse(null);
                        exam = execution != null
                                ? examServerController.getExamDetail(execution.getExamId()).orElse(null)
                                : null;
                    } else {
                        String examId = ExamResultsAccess.examIdFromPayload(request.getPayload());
                        exam = examId != null ? examServerController.getExamDetail(examId).orElse(null) : null;
                    }
                    String examAccessError = ExamResultsAccess.denyExamAccess(
                            request.getCommand(), exam, session);
                    if (examAccessError != null) {
                        return Response.failure(request.getCommand(), examAccessError, request.getRequestId());
                    }
                }
            }

            // Course-aware exam lock uses authenticated student, not a forged DTO id.
            if (request.getCommand() == Command.ASK_BOT_QUESTION
                    && request.getPayload() instanceof com.hsts.shared.net.dto.AskBotQuestionData botData
                    && session != null
                    && examServerController.hasActiveExamInCourse(session.getUserId(), botData.getCourseId())) {
                System.out.println("[EXAM-LOCK] blocked ASK_BOT for authenticated "
                        + session.getUserId() + " course=" + botData.getCourseId());
                return Response.failure(Command.ASK_BOT_QUESTION,
                        ActiveExamTracker.BOT_UNAVAILABLE_MESSAGE,
                        request.getRequestId());
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
            connectionRegistry.register(userId, user.getRole(), client);
            client.setInfo("userId", userId);
            if (!isBlank(user.getRole())) {
                client.setInfo("role", user.getRole());
            }
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