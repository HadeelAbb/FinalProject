package com.hsts.server;

import com.hsts.server.network.HSTSServer;
import com.hsts.server.network.ServerRequestRouter;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import server.controllers.LoginServerController;
import server.controllers.QuestionServerController;
import server.db.DatabaseManager;

/**
 * Production Central Entry Point for the HSTS Backend.
 * Dynamically queries MySQL for courses and maps client search operations over OCSF.
 */
public class MainServerApp {

    private static final int PORT = 3000; // Shared socket network port

    public static void main(String[] args) {
        System.out.println("=== INITIALIZING PRODUCTION HSTS CENTRAL SERVER ===");

        // 1. Initialize Partner 1's local MySQL connection pipeline
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.connect()) {
            System.err.println(">>> CRITICAL ERROR: Unable to connect to MySQL database. Aborting server startup.");
            return;
        }
        System.out.println("[DATABASE] Hard drive connection established successfully.");

        // 2. Instantiate Partner 1's backend logic controllers
        LoginServerController loginServerController = new LoginServerController();
        QuestionServerController questionServerController = new QuestionServerController();

        // 3. Setup Partner 2's functional Request Router framework
        ServerRequestRouter router = new ServerRequestRouter();

        // NEW: created here (before the route handlers below) instead of at
        // the bottom of main(), so CREATE_QUESTION / EDIT_QUESTION /
        // DELETE_QUESTION can call server.sendToAllClients(...) after a
        // successful change, letting every connected client refresh its
        // question list automatically instead of only the client that made
        // the change seeing the update.
        HSTSServer server = new HSTSServer(PORT);

        // =========================================================================
        // ROUTE A: LOGIN (Fully Dynamic Database Query)
        // =========================================================================
        router.registerHandler(Command.LOGIN, request -> {
            LoginData data = (LoginData) request.getPayload();

            // Invoke YOUR real credentials verification query method!
            boolean isAuthenticated = loginServerController.login(data.getUsername(), data.getPassword());

            if (!isAuthenticated) {
                return Response.failure(Command.LOGIN, "Invalid username or password", request.getRequestId());
            }

            // Create a structured Teacher entity object for the GUI mapping
            com.hsts.shared.model.Teacher teacherProfile = new com.hsts.shared.model.Teacher();
            teacherProfile.setId(data.getUsername());
            teacherProfile.setFirstName("Authenticated");
            teacherProfile.setLastName("Teacher");
            teacherProfile.setRole("TEACHER");

            // DYNAMIC SQL STREAM: Pull the courses directly from your live MySQL tables!
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

            // Attach the real database row entries directly to the login payload
            teacherProfile.setCourses(realDBCourses);

            return Response.success(Command.LOGIN, teacherProfile, "Database authentication success.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE A.1: LOGOUT (Releases the username so it can log in again)
        // =========================================================================
        // FIX: this route didn't exist before. LoginServerController.login()
        // refuses any username that's already marked active, but nothing
        // ever called logout() to clear that mark - so once a user logged
        // in, that username was permanently blocked until the whole server
        // process was restarted. This route, paired with the client now
        // actually sending Command.LOGOUT on window close, fixes that.
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

                    // Standard properties
                    guiQ.setQuestionId(dbQ.getQuestionId());
                    guiQ.setText(dbQ.getText());
                    guiQ.setTopic(dbQ.getTopic());
                    // FIX: previously hardcoded to Difficulty.MEDIUM for every result,
                    // ignoring the real difficulty value already fetched from MySQL.
                    com.hsts.shared.model.Difficulty realDifficulty;
                    try {
                        realDifficulty = com.hsts.shared.model.Difficulty.valueOf(dbQ.getDifficulty());
                    } catch (Exception ex) {
                        realDifficulty = com.hsts.shared.model.Difficulty.MEDIUM; // safe fallback if DB value is unexpected
                    }
                    guiQ.setDifficulty(realDifficulty);
                    guiQ.setCourseId(courseFilter);

                    // ─────────────────────────────────────────────────────────────────
                    // 🔥 CRITICAL FIXES FOR VIEW DETAILS:
                    // ─────────────────────────────────────────────────────────────────
                    // 1. Pass the real instructions text over the wire
                    guiQ.setInstructions(dbQ.getInstructions());

                    // 2. Map the actual SQL string array list to the custom QuestionAnswer DTO list
                    java.util.List<com.hsts.shared.model.QuestionAnswer> guiAnswers = new java.util.ArrayList<>();
                    if (dbQ.getAnswers() != null) {
                        java.util.List<String> texts = dbQ.getAnswers();
                        java.util.List<Boolean> flags = dbQ.getCorrectFlags();
                        for (int i = 0; i < texts.size(); i++) {
                            com.hsts.shared.model.QuestionAnswer guiAns = new com.hsts.shared.model.QuestionAnswer();
                            guiAns.setText(texts.get(i));
                            // FIX: previously isCorrect was never set, so every answer
                            // defaulted to false and the GUI couldn't flag the right one.
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
            // 1. Unpack the CreateQuestionData directly
            com.hsts.shared.net.dto.CreateQuestionData wrapperDto = (com.hsts.shared.net.dto.CreateQuestionData) request.getPayload();

            // 2. Extract values directly from the wrapperDto since it contains the getters!
            String text = wrapperDto.getText();
            String difficulty = wrapperDto.getDifficulty() != null ? wrapperDto.getDifficulty().toString() : "MEDIUM";
            String instructions = wrapperDto.getInstructions() != null ? wrapperDto.getInstructions() : "";
            String topic = wrapperDto.getTopic();
            String courseId = wrapperDto.getCourseId() != null ? wrapperDto.getCourseId() : "11";

            // ─────────────────────────────────────────────────────────────────
            // 🔥 NEW STEP: Extract the answers list from the DTO payload array
            // ─────────────────────────────────────────────────────────────────
            java.util.List<String> answersList = new java.util.ArrayList<>();
            // FIX: also extract the real correctness flag for each answer.
            // Previously this list didn't exist, so createQuestion() always
            // hardcoded answer index 1 as correct regardless of GUI selection.
            java.util.List<Integer> correctFlagsList = new java.util.ArrayList<>();
            if (wrapperDto.getAnswers() != null) {
                for (com.hsts.shared.model.QuestionAnswer ans : wrapperDto.getAnswers()) {
                    if (ans.getText() != null && !ans.getText().trim().isEmpty()) {
                        answersList.add(ans.getText());
                        correctFlagsList.add(ans.isCorrect() ? 1 : 0);
                    }
                }
            }

            // 3. Invoke YOUR controller's updated signature passing the answers list as the 6th parameter!
            // FIX: createQuestion now returns the generated questionId (or null on
            // failure) instead of boolean, so the real ID can be sent back to the GUI.
            String newQuestionId = questionServerController.createQuestion(text, difficulty, instructions, topic, courseId, answersList, correctFlagsList);

            if (newQuestionId == null) {
                return Response.failure(Command.CREATE_QUESTION, "Database insertion rejected the record.", request.getRequestId());
            }

            // ─────────────────────────────────────────────────────────────────
            // FIX: Build an actual Question object for the client instead of
            // echoing back the input wrapperDto. The client casts this payload
            // to com.hsts.shared.model.Question, so that's what must be sent.
            // ─────────────────────────────────────────────────────────────────
            com.hsts.shared.model.Question savedQ = new com.hsts.shared.model.Question();
            savedQ.setQuestionId(newQuestionId);
            savedQ.setText(text);
            savedQ.setInstructions(instructions);
            savedQ.setTopic(topic);
            savedQ.setCourseId(courseId);
            savedQ.setDifficulty(wrapperDto.getDifficulty());
            savedQ.setAnswers(wrapperDto.getAnswers());

            // NEW: notify every connected client that the question list
            // changed, so other open clients refresh automatically instead
            // of only this client seeing the new question.
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

            // Build separate lists for text strings and their matching correctness bits
            java.util.List<String> updatedAnswers = new java.util.ArrayList<>();
            java.util.List<Integer> correctnessBits = new java.util.ArrayList<>();

            if (wrapperDto.getAnswers() != null) {
                for (com.hsts.shared.model.QuestionAnswer ans : wrapperDto.getAnswers()) {
                    if (ans.getText() != null && !ans.getText().trim().isEmpty()) {
                        updatedAnswers.add(ans.getText());
                        // Capture Partner 3's live GUI Radio Button selection!
                        // If selected/correct, add 1, otherwise add 0.
                        correctnessBits.add(ans.isCorrect() ? 1 : 0);
                    }
                }
            }

            // Pass BOTH arrays to your controller method
            boolean isUpdated = questionServerController.editQuestion(questionId, newText, newInstruction, updatedAnswers, correctnessBits);

            if (!isUpdated) {
                return Response.failure(Command.EDIT_QUESTION, "Database update rejected the change.", request.getRequestId());
            }

            // ─────────────────────────────────────────────────────────────────
            // FIX: Build an actual Question object for the client instead of
            // echoing back the input wrapperDto. The client casts this payload
            // to com.hsts.shared.model.Question, so that's what must be sent.
            // ─────────────────────────────────────────────────────────────────
            com.hsts.shared.model.Question updatedQ = new com.hsts.shared.model.Question();
            updatedQ.setQuestionId(questionId);
            updatedQ.setText(newText);
            updatedQ.setInstructions(newInstruction);
            updatedQ.setTopic(wrapperDto.getTopic());
            // EditQuestionData has no courseId field (course is baked into the
            // questionId prefix, e.g. "11001" -> course "11"), so derive it here.
            updatedQ.setCourseId(questionId != null && questionId.length() >= 2 ? questionId.substring(0, 2) : null);
            updatedQ.setDifficulty(wrapperDto.getDifficulty());
            updatedQ.setAnswers(wrapperDto.getAnswers());

            // NEW: notify every connected client so other open clients
            // refresh automatically instead of only this client seeing
            // the edit.
            server.sendToAllClients(Response.success(Command.QUESTIONS_CHANGED, null, "A question was edited.", null));

            return Response.success(Command.EDIT_QUESTION, updatedQ, "Question updated in MySQL successfully!", request.getRequestId());
        });

        // =========================================================================
        // ROUTE E: DELETE QUESTION (Permanently removes an item from storage)
        // =========================================================================
        router.registerHandler(Command.DELETE_QUESTION, request -> {
            // Unpack the incoming deletion request DTO envelope packet
            com.hsts.shared.net.dto.DeleteQuestionData wrapperDto = (com.hsts.shared.net.dto.DeleteQuestionData) request.getPayload();

            // Invoke your cascading deletion controller method
            boolean isDeleted = questionServerController.deleteQuestion(wrapperDto.getQuestionId());

            if (!isDeleted) {
                return Response.failure(Command.DELETE_QUESTION, "Deletion command rejected by backend transaction.", request.getRequestId());
            }

            // Note: DELETE_QUESTION's client handler (QuestionClientController)
            // does not read the payload at all for this command, so returning
            // wrapperDto here is safe and does not need to change.

            // NEW: notify every connected client so other open clients
            // refresh automatically instead of only this client seeing
            // the question disappear.
            server.sendToAllClients(Response.success(Command.QUESTIONS_CHANGED, null, "A question was deleted.", null));

            return Response.success(Command.DELETE_QUESTION, wrapperDto, "Record dropped from database completely.", request.getRequestId());
        });

        // 4. Wire the router into the server (already created above) and start listening
        server.setRouter(router); // Inject the complete router switchboard into OCSF

        try {
            server.startServer();
            System.out.println(">>> PRODUCTION SYSTEM ENGINE LISTENING LIVE ON SOCKET PORT " + PORT + " <<<");
        } catch (Exception e) {
            System.err.println(">>> FATAL: OCSF initialization frame collapsed. <<<");
            e.printStackTrace();
        }
    }
}