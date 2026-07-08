package server.controllers;

import server.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Production Controller handling creation, modification, deletion, and search filtering of exam questions.
 */
public class QuestionServerController {

    private final DatabaseManager dbManager;

    public QuestionServerController() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Inserts a new question and its 4 answers under a safe database transaction.
     * FIX: now returns the generated questionId (or null on failure) instead of a
     * plain boolean, so callers (MainServerApp) can send the real ID back to the
     * GUI instead of leaving it null in the success response.
     */
    public String createQuestion(String text, String difficulty, String instructions, String topic, String courseId, java.util.List<String> answers) {
        return createQuestion(text, difficulty, instructions, topic, courseId, answers, null);
    }

    /**
     * Overload accepting an explicit correctness list (parallel to 'answers').
     * If correctFlags is null, falls back to the old hardcoded "index 1 is correct"
     * behavior for backward compatibility with existing callers (e.g. DatabaseTestDriver),
     * but any real caller should now pass the actual selections from the GUI.
     */
    public String createQuestion(String text, String difficulty, String instructions, String topic, String courseId,
                                  java.util.List<String> answers, java.util.List<Integer> correctFlags) {
        String questionId = generateQuestionId(courseId);
        if (questionId == null) return null;

        String sql = "INSERT INTO questions (question_id, text, difficulty, instructions, topic, course_id) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = dbManager.getConnection();
        if (conn == null) return null;

        try {
            conn.setAutoCommit(false);

            // 1. Insert Main Question Row
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, questionId);
                stmt.setString(2, text);
                stmt.setString(3, difficulty);
                stmt.setString(4, instructions);
                stmt.setString(5, topic);
                stmt.setString(6, courseId);
                stmt.executeUpdate();
            }

            // 2. Insert the 4 Linked Multiple Choice Options
            String answerSql = "INSERT INTO question_answers (question_id, answer_text, is_correct) VALUES (?, ?, ?)";
            try (PreparedStatement answerStmt = conn.prepareStatement(answerSql)) {
                for (int i = 0; i < answers.size(); i++) {
                    answerStmt.setString(1, questionId);
                    answerStmt.setString(2, answers.get(i));
                    // FIX: previously always hardcoded index 1 as the correct answer
                    // regardless of what the GUI actually selected. Now uses the real
                    // correctness flag passed in, falling back to the old behavior
                    // only if no flags were supplied at all (legacy callers).
                    int isCorrect;
                    if (correctFlags != null && i < correctFlags.size()) {
                        isCorrect = correctFlags.get(i);
                    } else {
                        isCorrect = (i == 1) ? 1 : 0;
                    }
                    answerStmt.setInt(3, isCorrect);
                    answerStmt.addBatch();
                }
                answerStmt.executeBatch();
            }

            conn.commit();
            return questionId;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackEx) { rollbackEx.printStackTrace(); }
            e.printStackTrace();
            return null;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Modifies the question, instructions, and resets the multiple-choice lines.
     */
    public boolean editQuestion(String questionId, String newText, String newInstruction, java.util.List<String> updatedAnswers, java.util.List<Integer> correctnessBits) {
        String sql = "UPDATE questions SET text = ?, instructions = ? WHERE question_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            // 1. Update main properties
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newText);
                stmt.setString(2, newInstruction);
                stmt.setString(3, questionId);
                stmt.executeUpdate();
            }

            // 2. Wipe old answers safely
            String deleteAnswersSql = "DELETE FROM question_answers WHERE question_id = ?";
            try (PreparedStatement delStmt = conn.prepareStatement(deleteAnswersSql)) {
                delStmt.setString(1, questionId);
                delStmt.executeUpdate();
            }

            // 3. Write fresh updated multiple choice option lines with real states!
            String insertAnswerSql = "INSERT INTO question_answers (question_id, answer_text, is_correct) VALUES (?, ?, ?)";
            try (PreparedStatement answerStmt = conn.prepareStatement(insertAnswerSql)) {
                for (int i = 0; i < updatedAnswers.size(); i++) {
                    answerStmt.setString(1, questionId);
                    answerStmt.setString(2, updatedAnswers.get(i));
                    // Dynamic value pulled straight from the UI selection array!
                    answerStmt.setInt(3, correctnessBits.get(i));
                    answerStmt.addBatch();
                }
                answerStmt.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackEx) { rollbackEx.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Permanently deletes a specific question row from the database using its ID.
     */
    public boolean deleteQuestion(String questionId) {
        // Safe cascading behavior: clean dependent rows first before dropping main reference
        String deleteAnswersSql = "DELETE FROM question_answers WHERE question_id = ?";
        String deleteQuestionSql = "DELETE FROM questions WHERE question_id = ?";

        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt1 = conn.prepareStatement(deleteAnswersSql)) {
                stmt1.setString(1, questionId);
                stmt1.executeUpdate();
            }

            int rowsAffected;
            try (PreparedStatement stmt2 = conn.prepareStatement(deleteQuestionSql)) {
                stmt2.setString(1, questionId);
                rowsAffected = stmt2.executeUpdate();
            }

            conn.commit();
            return rowsAffected > 0;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackEx) { rollbackEx.printStackTrace(); }
            System.err.println("[DB ERROR] Failed to drop question: " + questionId);
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Searches for questions filtering by a specific topic and course ID.
     */
    public java.util.List<shared.entities.Question> searchQuestions(String topicKeyword, String courseId) {
        java.util.List<shared.entities.Question> resultsList = new java.util.ArrayList<>();
        String sql = "SELECT question_id, text, instructions, difficulty, topic FROM questions WHERE course_id = ? AND topic LIKE ?";
        Connection conn = dbManager.getConnection();

        if (conn == null) return resultsList;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            stmt.setString(2, "%" + (topicKeyword != null ? topicKeyword : "") + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("question_id");
                    String text = rs.getString("text");
                    String instructions = rs.getString("instructions");
                    String difficulty = rs.getString("difficulty");
                    String topic = rs.getString("topic");

                    shared.entities.Question questionObj = new shared.entities.Question(id, text, instructions, difficulty, topic);

                    java.util.List<String> answerTexts = new java.util.ArrayList<>();
                    // FIX: also select is_correct so the GUI can flag the right answer.
                    // Previously only answer_text was selected, so every answer came
                    // back as "not correct" regardless of what was stored in MySQL.
                    java.util.List<Boolean> correctFlags = new java.util.ArrayList<>();
                    String answerSql = "SELECT answer_text, is_correct FROM question_answers WHERE question_id = ?";

                    try (PreparedStatement answerStmt = conn.prepareStatement(answerSql)) {
                        answerStmt.setString(1, id);
                        try (ResultSet answerRs = answerStmt.executeQuery()) {
                            while (answerRs.next()) {
                                answerTexts.add(answerRs.getString("answer_text"));
                                correctFlags.add(answerRs.getInt("is_correct") == 1);
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("[DB WARNING] Could not pull answers for ID: " + id);
                    }

                    questionObj.setAnswers(answerTexts);
                    questionObj.setCorrectFlags(correctFlags);
                    resultsList.add(questionObj);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed fetching search query results.");
            e.printStackTrace();
        }

        return resultsList;
    }

    /**
     * Automatically generates a structural 5-digit ID matching Section 3.1 specifications.
     */
    private String generateQuestionId(String courseId) {
        int parsedCourseNum = Integer.parseInt(courseId);
        String coursePrefix = String.format("%02d", parsedCourseNum);

        String sql = "SELECT COUNT(*) FROM questions WHERE course_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int existingCount = rs.getInt(1);
                    int nextSerialNumber = existingCount + 1;

                    String counterSuffix = String.format("%03d", nextSerialNumber);
                    return coursePrefix + counterSuffix;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ID GEN ERROR] Failed calculating sequence metadata count.");
            e.printStackTrace();
        }
        return null;
    }
}