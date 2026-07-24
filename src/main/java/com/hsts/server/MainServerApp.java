package com.hsts.server;

import com.hsts.server.network.HSTSServer;
import com.hsts.server.network.ServerRequestRouter;
import com.hsts.shared.model.BotInteraction;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.CreateExamAutoData;
import com.hsts.shared.net.dto.CreateExamManualData;
import com.hsts.shared.net.dto.ExamApprovalDecisionData;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import com.hsts.shared.net.dto.SubmitExamData;
import server.controllers.ExamServerController;
import server.controllers.LoginServerController;
import server.controllers.QuestionServerController;
import server.db.DatabaseManager;
import server.db.repository.BotRepositoryImpl;

import java.util.List;
import java.util.UUID;

/**
 * Production Central Entry Point for the HSTS Backend.
 * Dynamically queries MySQL for courses and maps client search operations over OCSF.
 */
public class MainServerApp {

    private static final int PORT = 3000; // Shared socket network port

    public static void main(String[] args) {
        System.out.println("=== INITIALIZING PRODUCTION HSTS CENTRAL SERVER ===");

        // 1. Initialize local MySQL connection pipeline
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.connect()) {
            System.err.println(">>> CRITICAL ERROR: Unable to connect to MySQL database. Aborting server startup.");
            return;
        }
        System.out.println("[DATABASE] Hard drive connection established successfully.");

        // 2. Instantiate backend logic controllers & repositories
        LoginServerController loginServerController = new LoginServerController();
        QuestionServerController questionServerController = new QuestionServerController();
        ExamServerController examServerController = new ExamServerController();
        BotRepositoryImpl botRepository = new BotRepositoryImpl();

        // 3. Setup functional Request Router framework
        ServerRequestRouter router = new ServerRequestRouter();
        HSTSServer server = new HSTSServer(PORT);

        // =========================================================================
        // ROUTE A: LOGIN (Fully Dynamic Database Query)
        // =========================================================================
        router.registerHandler(Command.LOGIN, request -> {
            LoginData data = (LoginData) request.getPayload();

            boolean isAuthenticated = loginServerController.login(data.getUsername(), data.getPassword());

            if (!isAuthenticated) {
                return Response.failure(Command.LOGIN, "Invalid username or password", request.getRequestId());
            }

            com.hsts.shared.model.Teacher teacherProfile = new com.hsts.shared.model.Teacher();
            teacherProfile.setId(data.getUsername());
            teacherProfile.setFirstName("Authenticated");
            teacherProfile.setLastName("Teacher");
            teacherProfile.setRole("TEACHER");

            java.util.List<com.hsts.shared.model.Course> realDBCourses = new java.util.ArrayList<>();

            try {
                java.sql.Connection conn = DatabaseManager.getInstance().getConnection();
                String sql = "SELECT course_id, name FROM courses";

                try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
                     java.sql.ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        String id = rs.getString("course_id");
                        String name = rs.getString("name");
                        realDBCourses.add(new com.hsts.shared.model.Course(id, name));
                    }
                }
            } catch (java.sql.SQLException e) {
                System.err.println("[SERVER-ERROR] Failed to fetch real courses from SQL:");
                e.printStackTrace();
            }

            teacherProfile.setCourses(realDBCourses);

            return Response.success(Command.LOGIN, teacherProfile, "Database authentication success.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE A.1: LOGOUT (Releases the username so it can log in again)
        // =========================================================================
        router.registerHandler(Command.LOGOUT, request -> {
            com.hsts.shared.net.dto.LogoutData data = (com.hsts.shared.net.dto.LogoutData) request.getPayload();
            loginServerController.logout(data.getUsername());
            return Response.success(Command.LOGOUT, null, "Logged out successfully.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE B: SEARCH QUESTIONS (Dual-Filter Data-Type Standardized Mapping)
        // =========================================================================
        router.registerHandler(Command.SEARCH_QUESTIONS, request -> {
            SearchQuestionsData data = (SearchQuestionsData) request.getPayload();

            String courseFilter = data.getCourseId() != null ? data.getCourseId() : "11";
            java.util.List<shared.entities.Question> rawDbResults = questionServerController.searchQuestions(data.getTopic(), courseFilter);
            java.util.List<com.hsts.shared.model.Question> formattedGuiResults = new java.util.ArrayList<>();

            if (rawDbResults != null) {
                for (shared.entities.Question dbQ : rawDbResults) {
                    com.hsts.shared.model.Question guiQ = new com.hsts.shared.model.Question();

                    guiQ.setQuestionId(dbQ.getQuestionId());
                    guiQ.setText(dbQ.getText());
                    guiQ.setTopic(dbQ.getTopic());

                    com.hsts.shared.model.Difficulty realDifficulty;
                    try {
                        realDifficulty = com.hsts.shared.model.Difficulty.valueOf(dbQ.getDifficulty());
                    } catch (Exception ex) {
                        realDifficulty = com.hsts.shared.model.Difficulty.MEDIUM;
                    }
                    guiQ.setDifficulty(realDifficulty);
                    guiQ.setCourseId(courseFilter);
                    guiQ.setInstructions(dbQ.getInstructions());

                    java.util.List<com.hsts.shared.model.QuestionAnswer> guiAnswers = new java.util.ArrayList<>();
                    if (dbQ.getAnswers() != null) {
                        java.util.List<String> texts = dbQ.getAnswers();
                        java.util.List<Boolean> flags = dbQ.getCorrectFlags();
                        for (int i = 0; i < texts.size(); i++) {
                            com.hsts.shared.model.QuestionAnswer guiAns = new com.hsts.shared.model.QuestionAnswer();
                            guiAns.setText(texts.get(i));
                            boolean isCorrect = flags != null && i < flags.size() && Boolean.TRUE.equals(flags.get(i));
                            guiAns.setCorrect(isCorrect);
                            guiAnswers.add(guiAns);
                        }
                    }
                    guiQ.setAnswers(guiAnswers);

                    formattedGuiResults.add(guiQ);
                }
            }

            return Response.success(Command.SEARCH_QUESTIONS, formattedGuiResults, "Database query loaded successfully.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE C: CREATE QUESTION (Saves main row AND 4 answers to MySQL)
        // =========================================================================
        router.registerHandler(Command.CREATE_QUESTION, request -> {
            com.hsts.shared.net.dto.CreateQuestionData wrapperDto = (com.hsts.shared.net.dto.CreateQuestionData) request.getPayload();

            String text = wrapperDto.getText();
            String difficulty = wrapperDto.getDifficulty() != null ? wrapperDto.getDifficulty().toString() : "MEDIUM";
            String instructions = wrapperDto.getInstructions() != null ? wrapperDto.getInstructions() : "";
            String topic = wrapperDto.getTopic();
            String courseId = wrapperDto.getCourseId() != null ? wrapperDto.getCourseId() : "11";

            java.util.List<String> answersList = new java.util.ArrayList<>();
            java.util.List<Integer> correctFlagsList = new java.util.ArrayList<>();
            if (wrapperDto.getAnswers() != null) {
                for (com.hsts.shared.model.QuestionAnswer ans : wrapperDto.getAnswers()) {
                    if (ans.getText() != null && !ans.getText().trim().isEmpty()) {
                        answersList.add(ans.getText());
                        correctFlagsList.add(ans.isCorrect() ? 1 : 0);
                    }
                }
            }

            String newQuestionId = questionServerController.createQuestion(text, difficulty, instructions, topic, courseId, answersList, correctFlagsList);

            if (newQuestionId == null) {
                return Response.failure(Command.CREATE_QUESTION, "Database insertion rejected the record.", request.getRequestId());
            }

            com.hsts.shared.model.Question savedQ = new com.hsts.shared.model.Question();
            savedQ.setQuestionId(newQuestionId);
            savedQ.setText(text);
            savedQ.setInstructions(instructions);
            savedQ.setTopic(topic);
            savedQ.setCourseId(courseId);
            savedQ.setDifficulty(wrapperDto.getDifficulty());
            savedQ.setAnswers(wrapperDto.getAnswers());

            server.sendToAllClients(Response.success(Command.QUESTIONS_CHANGED, null, "A question was created.", null));

            return Response.success(Command.CREATE_QUESTION, savedQ, "Question saved to MySQL database successfully!", request.getRequestId());
        });

        // =========================================================================
        // ROUTE D: EDIT QUESTION (Dynamically tracks correct answer choices)
        // =========================================================================
        router.registerHandler(Command.EDIT_QUESTION, request -> {
            com.hsts.shared.net.dto.EditQuestionData wrapperDto = (com.hsts.shared.net.dto.EditQuestionData) request.getPayload();

            String questionId = wrapperDto.getQuestionId();
            String newText = wrapperDto.getText();
            String newInstruction = wrapperDto.getInstructions() != null ? wrapperDto.getInstructions() : "";

            java.util.List<String> updatedAnswers = new java.util.ArrayList<>();
            java.util.List<Integer> correctnessBits = new java.util.ArrayList<>();

            if (wrapperDto.getAnswers() != null) {
                for (com.hsts.shared.model.QuestionAnswer ans : wrapperDto.getAnswers()) {
                    if (ans.getText() != null && !ans.getText().trim().isEmpty()) {
                        updatedAnswers.add(ans.getText());
                        correctnessBits.add(ans.isCorrect() ? 1 : 0);
                    }
                }
            }

            boolean isUpdated = questionServerController.editQuestion(questionId, newText, newInstruction, updatedAnswers, correctnessBits);

            if (!isUpdated) {
                return Response.failure(Command.EDIT_QUESTION, "Database update rejected the change.", request.getRequestId());
            }

            com.hsts.shared.model.Question updatedQ = new com.hsts.shared.model.Question();
            updatedQ.setQuestionId(questionId);
            updatedQ.setText(newText);
            updatedQ.setInstructions(newInstruction);
            updatedQ.setTopic(wrapperDto.getTopic());
            updatedQ.setCourseId(questionId != null && questionId.length() >= 2 ? questionId.substring(0, 2) : null);
            updatedQ.setDifficulty(wrapperDto.getDifficulty());
            updatedQ.setAnswers(wrapperDto.getAnswers());

            server.sendToAllClients(Response.success(Command.QUESTIONS_CHANGED, null, "A question was edited.", null));

            return Response.success(Command.EDIT_QUESTION, updatedQ, "Question updated in MySQL successfully!", request.getRequestId());
        });

        // =========================================================================
        // ROUTE E: DELETE QUESTION (Permanently removes an item from storage)
        // =========================================================================
        router.registerHandler(Command.DELETE_QUESTION, request -> {
            com.hsts.shared.net.dto.DeleteQuestionData wrapperDto = (com.hsts.shared.net.dto.DeleteQuestionData) request.getPayload();

            boolean isDeleted = questionServerController.deleteQuestion(wrapperDto.getQuestionId());

            if (!isDeleted) {
                return Response.failure(Command.DELETE_QUESTION, "Deletion command rejected by backend transaction.", request.getRequestId());
            }

            server.sendToAllClients(Response.success(Command.QUESTIONS_CHANGED, null, "A question was deleted.", null));

            return Response.success(Command.DELETE_QUESTION, wrapperDto, "Record dropped from database completely.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE F: CREATE EXAM MANUAL (Manual question selection)
        // =========================================================================
        router.registerHandler(Command.CREATE_EXAM_MANUAL, request -> {
            CreateExamManualData data = (CreateExamManualData) request.getPayload();
            Exam exam = examServerController.createManualExam(data);

            if (exam == null) {
                return Response.failure(Command.CREATE_EXAM_MANUAL, "Failed to create manual exam.", request.getRequestId());
            }
            return Response.success(Command.CREATE_EXAM_MANUAL, exam, "Draft exam created successfully.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE G: CREATE EXAM AUTO (Rule-based automatic generation)
        // =========================================================================
        router.registerHandler(Command.CREATE_EXAM_AUTO, request -> {
            CreateExamAutoData data = (CreateExamAutoData) request.getPayload();
            Exam exam = examServerController.createAutoExam(data);

            if (exam == null) {
                return Response.failure(Command.CREATE_EXAM_AUTO, "Insufficient matching questions for criteria.", request.getRequestId());
            }
            return Response.success(Command.CREATE_EXAM_AUTO, exam, "Auto-generated exam created successfully.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE H: APPROVE / REJECT EXAM (Subject Coordinator Decision)
        // =========================================================================
        router.registerHandler(Command.APPROVE_EXAM, request -> {
            ExamApprovalDecisionData data = (ExamApprovalDecisionData) request.getPayload();
            Exam resultExam;

            boolean isApproved = data.getReason() == null || data.getReason().isBlank();

            if (isApproved) {
                resultExam = examServerController.approveExam(data.getExamId(), data.getCoordinatorId());
            } else {
                resultExam = examServerController.rejectExam(data.getExamId(), data.getCoordinatorId(), data.getReason());
            }

            if (resultExam == null) {
                return Response.failure(Command.APPROVE_EXAM, "Exam decision processing failed.", request.getRequestId());
            }

            return Response.success(Command.APPROVE_EXAM, resultExam, "Exam decision recorded.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE I: SUBMIT EXAM (Student submission & automatic scoring)
        // =========================================================================
        router.registerHandler(Command.SUBMIT_EXAM, request -> {
            SubmitExamData data = (SubmitExamData) request.getPayload();
            ExamAnswer answer = examServerController.submitExam(data);

            if (answer == null) {
                return Response.failure(Command.SUBMIT_EXAM, "Exam submission failed.", request.getRequestId());
            }
            return Response.success(Command.SUBMIT_EXAM, answer, "Exam submitted and scored automatically.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE J: CONFIRM GRADE (Teacher grade override & feedback)
        // =========================================================================
        router.registerHandler(Command.CONFIRM_GRADE, request -> {
            ConfirmGradeData data = (ConfirmGradeData) request.getPayload();
            boolean isSuccess = examServerController.confirmGrade(data);

            if (!isSuccess) {
                return Response.failure(Command.CONFIRM_GRADE, "Failed to confirm grade in database.", request.getRequestId());
            }
            return Response.success(Command.CONFIRM_GRADE, true, "Grade confirmed successfully.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE K: ASK_BOT_QUESTION (Process student study bot question)
        // =========================================================================
        router.registerHandler(Command.ASK_BOT_QUESTION, (request) -> {
            try {
                if (request.getPayload() instanceof BotInteraction incoming) {

                    if (incoming.getInteractionId() == null || incoming.getInteractionId().isEmpty()) {
                        incoming.setInteractionId("BOT-" + UUID.randomUUID().toString().substring(0, 8));
                    }

                    if (incoming.getAnswer() == null || incoming.getAnswer().isEmpty()) {
                        incoming.setAnswer("Automated Assistant: I received your question regarding course "
                                + incoming.getCourseId() + ". Please review course materials or consult your instructor!");
                    }

                    botRepository.save(incoming);

                    System.out.println("🤖 Study Bot answered student " + incoming.getStudentId() + " successfully.");
                    return Response.success(Command.ASK_BOT_QUESTION, incoming, "Bot interaction recorded.", request.getRequestId());
                }
                return Response.failure(Command.ASK_BOT_QUESTION, "Invalid payload for ASK_BOT_QUESTION.", request.getRequestId());
            } catch (Exception e) {
                return Response.failure(Command.ASK_BOT_QUESTION, "Database error: " + e.getMessage(), request.getRequestId());
            }
        });

        // =========================================================================
        // ROUTE L: GET_BOT_HISTORY (Retrieve chat history for student)
        // =========================================================================
        router.registerHandler(Command.GET_BOT_HISTORY, (request) -> {
            try {
                if (request.getPayload() instanceof String studentId) {
                    List<BotInteraction> history = botRepository.findByStudentId(studentId);
                    return Response.success(Command.GET_BOT_HISTORY, history, "Bot history fetched.", request.getRequestId());
                }
                return Response.failure(Command.GET_BOT_HISTORY, "Student ID must be a String.", request.getRequestId());
            } catch (Exception e) {
                return Response.failure(Command.GET_BOT_HISTORY, "Database error: " + e.getMessage(), request.getRequestId());
            }
        });

        // 4. Wire the router into the server and start listening
        server.setRouter(router);

        try {
            server.startServer();
            System.out.println(">>> PRODUCTION SYSTEM ENGINE LISTENING LIVE ON SOCKET PORT " + PORT + " <<<");
        } catch (Exception e) {
            System.err.println(">>> FATAL: OCSF initialization frame collapsed. <<<");
            e.printStackTrace();
        }
    }
}