package server.controllers;

import server.db.DatabaseManager;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

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
        return createQuestion(text, difficulty, instructions, topic, courseId, answers, correctFlags, null, null);
    }

    public String createQuestion(String text, String difficulty, String instructions, String topic, String courseId,
                                  java.util.List<String> answers, java.util.List<Integer> correctFlags,
                                  byte[] imageData, String imageFilename) {
        if (QuestionCreateValidator.validateAnswerLists(answers, correctFlags) != null) {
            return null;
        }
        if (com.hsts.shared.model.QuestionIllustration.validate(imageData, imageFilename) != null) {
            return null;
        }

        String questionId = generateQuestionId(courseId);
        if (questionId == null) return null;

        String sql = "INSERT INTO questions (question_id, text, difficulty, instructions, topic, course_id, "
                + "root_question_id, version_number, is_latest, image_filename, image_data) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?)";
        Connection conn = dbManager.getConnection();
        if (conn == null) return null;
        ensureVersionColumns(conn);

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
                stmt.setString(7, questionId);
                bindIllustration(stmt, 8, 9, imageData, imageFilename);
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
     * Creates the next immutable version of a question. The source row is left
     * unchanged (text, answers, exams that reference it stay on that physical id).
     */
    public VersionEditResult createNextVersion(String sourceQuestionId, String newText, String newInstruction,
                                               String newDifficulty, String newTopic,
                                               java.util.List<String> updatedAnswers,
                                               java.util.List<Integer> correctnessBits) {
        return createNextVersion(sourceQuestionId, newText, newInstruction, newDifficulty, newTopic,
                updatedAnswers, correctnessBits, null, null);
    }

    public VersionEditResult createNextVersion(String sourceQuestionId, String newText, String newInstruction,
                                               String newDifficulty, String newTopic,
                                               java.util.List<String> updatedAnswers,
                                               java.util.List<Integer> correctnessBits,
                                               byte[] imageData, String imageFilename) {
        if (QuestionCreateValidator.validateAnswerLists(updatedAnswers, correctnessBits) != null) {
            return VersionEditResult.fail("A question must contain exactly 4 answers and exactly one correct answer.");
        }
        String imageError = com.hsts.shared.model.QuestionIllustration.validate(imageData, imageFilename);
        if (imageError != null) {
            return VersionEditResult.fail(imageError);
        }
        Connection conn = dbManager.getConnection();
        if (conn == null) {
            return VersionEditResult.fail("Database connection is unavailable.");
        }
        ensureVersionColumns(conn);

        try {
            conn.setAutoCommit(false);

            String sourceSql = "SELECT question_id, course_id, difficulty, topic, root_question_id, version_number, is_latest, "
                    + "image_filename, image_data FROM questions WHERE question_id = ? FOR UPDATE";
            String courseId;
            String rootId;
            String difficulty;
            String topic;
            boolean sourceIsLatest;
            byte[] sourceImageData = null;
            String sourceImageFilename = null;
            try (PreparedStatement stmt = conn.prepareStatement(sourceSql)) {
                stmt.setString(1, sourceQuestionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return VersionEditResult.fail("That question could not be found.");
                    }
                    courseId = rs.getString("course_id");
                    String storedRoot = rs.getString("root_question_id");
                    rootId = storedRoot != null && !storedRoot.isBlank() ? storedRoot : sourceQuestionId;
                    difficulty = newDifficulty != null && !newDifficulty.isBlank()
                            ? newDifficulty : rs.getString("difficulty");
                    topic = newTopic != null ? newTopic : rs.getString("topic");
                    sourceIsLatest = rs.getInt("is_latest") == 1;
                    sourceImageData = rs.getBytes("image_data");
                    sourceImageFilename = rs.getString("image_filename");
                }
            }

            if (!sourceIsLatest) {
                conn.rollback();
                return VersionEditResult.fail(QuestionVersioning.HISTORICAL_NOT_EDITABLE);
            }

            int maxVersion = 1;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT MAX(version_number) FROM questions WHERE root_question_id = ? FOR UPDATE")) {
                stmt.setString(1, rootId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        maxVersion = rs.getInt(1);
                    }
                }
            }
            int nextVersion = QuestionVersioning.nextVersionNumber(maxVersion);

            String newQuestionId = generateQuestionId(courseId);
            if (newQuestionId == null) {
                conn.rollback();
                return VersionEditResult.fail("Could not allocate a new question id.");
            }

            byte[] nextImageData = com.hsts.shared.model.QuestionIllustration.hasData(imageData)
                    ? imageData : sourceImageData;
            String nextImageFilename = com.hsts.shared.model.QuestionIllustration.hasData(imageData)
                    ? imageFilename : sourceImageFilename;

            String insertSql = "INSERT INTO questions (question_id, text, difficulty, instructions, topic, course_id, "
                    + "root_question_id, version_number, is_latest, image_filename, image_data) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, newQuestionId);
                stmt.setString(2, newText);
                stmt.setString(3, difficulty);
                stmt.setString(4, newInstruction);
                stmt.setString(5, topic);
                stmt.setString(6, courseId);
                stmt.setString(7, rootId);
                stmt.setInt(8, nextVersion);
                bindIllustration(stmt, 9, 10, nextImageData, nextImageFilename);
                stmt.executeUpdate();
            }

            String answerSql = "INSERT INTO question_answers (question_id, answer_text, is_correct) VALUES (?, ?, ?)";
            try (PreparedStatement answerStmt = conn.prepareStatement(answerSql)) {
                for (int i = 0; i < updatedAnswers.size(); i++) {
                    answerStmt.setString(1, newQuestionId);
                    answerStmt.setString(2, updatedAnswers.get(i));
                    answerStmt.setInt(3, correctnessBits.get(i));
                    answerStmt.addBatch();
                }
                answerStmt.executeBatch();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE questions SET is_latest = 0 WHERE root_question_id = ? AND question_id <> ?")) {
                stmt.setString(1, rootId);
                stmt.setString(2, newQuestionId);
                stmt.executeUpdate();
            }

            conn.commit();
            return VersionEditResult.ok(newQuestionId, nextVersion, rootId, courseId, nextImageData, nextImageFilename);
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackEx) { rollbackEx.printStackTrace(); }
            e.printStackTrace();
            return VersionEditResult.fail("Database update rejected the change.");
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static final class VersionEditResult {
        public final boolean success;
        public final String errorMessage;
        public final String newQuestionId;
        public final int newVersionNumber;
        public final String rootQuestionId;
        public final String courseId;
        public final byte[] imageData;
        public final String imageFilename;

        private VersionEditResult(boolean success, String errorMessage, String newQuestionId,
                                  int newVersionNumber, String rootQuestionId, String courseId,
                                  byte[] imageData, String imageFilename) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.newQuestionId = newQuestionId;
            this.newVersionNumber = newVersionNumber;
            this.rootQuestionId = rootQuestionId;
            this.courseId = courseId;
            this.imageData = imageData;
            this.imageFilename = imageFilename;
        }

        public static VersionEditResult ok(String newQuestionId, int newVersionNumber,
                                           String rootQuestionId, String courseId) {
            return ok(newQuestionId, newVersionNumber, rootQuestionId, courseId, null, null);
        }

        public static VersionEditResult ok(String newQuestionId, int newVersionNumber,
                                           String rootQuestionId, String courseId,
                                           byte[] imageData, String imageFilename) {
            return new VersionEditResult(true, null, newQuestionId, newVersionNumber, rootQuestionId, courseId,
                    imageData, imageFilename);
        }

        public static VersionEditResult fail(String errorMessage) {
            return new VersionEditResult(false, errorMessage, null, 0, null, null, null, null);
        }
    }

    /**
     * Deletes one physical question version if no exam still references it.
     * @return null on success, otherwise a failure message (no rows deleted)
     */
    public String deleteQuestion(String questionId) {
        Connection conn = dbManager.getConnection();
        if (conn == null) {
            return "Database connection is unavailable.";
        }
        ensureVersionColumns(conn);

        String deleteAnswersSql = "DELETE FROM question_answers WHERE question_id = ?";
        String deleteQuestionSql = "DELETE FROM questions WHERE question_id = ?";

        try {
            conn.setAutoCommit(false);

            if (isReferencedByExamQuestions(conn, questionId)) {
                conn.rollback();
                return QuestionVersioning.USED_BY_EXAM;
            }

            String rootId = null;
            boolean wasLatest = false;
            try (PreparedStatement meta = conn.prepareStatement(
                    "SELECT root_question_id, is_latest FROM questions WHERE question_id = ?")) {
                meta.setString(1, questionId);
                try (ResultSet rs = meta.executeQuery()) {
                    if (rs.next()) {
                        String storedRoot = rs.getString("root_question_id");
                        rootId = storedRoot != null && !storedRoot.isBlank() ? storedRoot : questionId;
                        wasLatest = rs.getInt("is_latest") == 1;
                    }
                }
            }

            try (PreparedStatement stmt1 = conn.prepareStatement(deleteAnswersSql)) {
                stmt1.setString(1, questionId);
                stmt1.executeUpdate();
            }

            int rowsAffected;
            try (PreparedStatement stmt2 = conn.prepareStatement(deleteQuestionSql)) {
                stmt2.setString(1, questionId);
                rowsAffected = stmt2.executeUpdate();
            }

            if (wasLatest && rootId != null) {
                try (PreparedStatement promote = conn.prepareStatement(
                        "UPDATE questions SET is_latest = 1 WHERE root_question_id = ? "
                                + "ORDER BY version_number DESC LIMIT 1")) {
                    promote.setString(1, rootId);
                    promote.executeUpdate();
                }
            }

            conn.commit();
            return rowsAffected > 0 ? null : "That question could not be found.";
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackEx) { rollbackEx.printStackTrace(); }
            System.err.println("[DB ERROR] Failed to drop question: " + questionId);
            e.printStackTrace();
            return "Deletion command rejected by backend transaction.";
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private boolean isReferencedByExamQuestions(Connection conn, String questionId) {
        if (questionId == null || questionId.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM exam_questions WHERE question_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, questionId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed checking exam_questions references: " + e.getMessage());
            return true;
        }
    }

    /**
     * Searches for questions filtering by an optional topic and required course ID.
     */
    public java.util.List<shared.entities.Question> searchQuestions(String topicKeyword, String courseId) {
        return searchQuestions(topicKeyword, courseId, null);
    }

    /**
     * Topic/course/difficulty are optional. A blank courseId returns every course
     * (Principal all-bank view). Teacher SEARCH_QUESTIONS still passes a course id.
     */
    public java.util.List<shared.entities.Question> searchQuestions(String topicKeyword, String courseId,
                                                                    String difficulty) {
        return searchQuestions(topicKeyword, courseId, difficulty, false);
    }

    public java.util.List<shared.entities.Question> searchQuestions(String topicKeyword, String courseId,
                                                                    String difficulty, boolean latestOnly) {
        java.util.List<shared.entities.Question> resultsList = new java.util.ArrayList<>();
        Connection conn = dbManager.getConnection();
        if (conn == null) return resultsList;
        ensureVersionColumns(conn);

        StringBuilder sql = new StringBuilder(
                "SELECT question_id, text, instructions, difficulty, topic, course_id, "
                        + "root_question_id, version_number, is_latest, image_filename, image_data "
                        + "FROM questions WHERE topic LIKE ?");
        if (courseId != null && !courseId.isBlank()) {
            sql.append(" AND course_id = ?");
        }
        if (difficulty != null && !difficulty.isBlank()) {
            sql.append(" AND difficulty = ?");
        }
        if (latestOnly) {
            sql.append(" AND is_latest = 1");
        }
        sql.append(" ORDER BY course_id, root_question_id, version_number, question_id");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int index = 1;
            stmt.setString(index++, "%" + (topicKeyword != null ? topicKeyword : "") + "%");
            if (courseId != null && !courseId.isBlank()) {
                stmt.setString(index++, courseId);
            }
            if (difficulty != null && !difficulty.isBlank()) {
                stmt.setString(index, difficulty);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("question_id");
                    String text = rs.getString("text");
                    String instructions = rs.getString("instructions");
                    String rowDifficulty = rs.getString("difficulty");
                    String topic = rs.getString("topic");

                    shared.entities.Question questionObj = new shared.entities.Question(id, text, instructions, rowDifficulty, topic);
                    questionObj.setCourseId(rs.getString("course_id"));
                    String rootId = rs.getString("root_question_id");
                    questionObj.setRootQuestionId(rootId != null && !rootId.isBlank() ? rootId : id);
                    questionObj.setVersionNumber(rs.getInt("version_number"));
                    questionObj.setLatest(rs.getInt("is_latest") == 1);
                    questionObj.setImageFilename(rs.getString("image_filename"));
                    questionObj.setImageData(rs.getBytes("image_data"));

                    java.util.List<String> answerTexts = new java.util.ArrayList<>();
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

    /** Actual course of an existing question. Used for EDIT/DELETE authorization. */
    public String findCourseId(String questionId) {
        if (questionId == null || questionId.isBlank()) {
            return null;
        }
        String sql = "SELECT course_id FROM questions WHERE question_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) {
            return null;
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, questionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("course_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to load question course: " + e.getMessage());
        }
        return null;
    }

    /**
     * Generates a unique question_id that fits questions.question_id VARCHAR(10).
     * Format: {courseId} + zero-padded per-course sequence (3 digits when that
     * still fits). Examples: CS101001, MATH201001, and legacy numeric 11001.
     * Uses the real course_id string — never Integer.parseInt(courseId).
     * Sequence is the max existing suffix for this course, not COUNT(*), so
     * deleted rows do not reuse an occupied primary key.
     */
    private String generateQuestionId(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return null;
        }

        int sequenceWidth = Math.min(3, 10 - courseId.length());
        if (sequenceWidth < 1) {
            System.err.println("[ID GEN ERROR] Course ID is too long to append a sequence: " + courseId);
            return null;
        }

        String sql = "SELECT question_id FROM questions WHERE course_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);

            int maxSeq = 0;
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer seq = parseSequenceSuffix(rs.getString("question_id"), courseId);
                    if (seq != null) {
                        maxSeq = Math.max(maxSeq, seq);
                    }
                }
            }

            int nextSeq = maxSeq + 1;
            int maxValue = (int) Math.pow(10, sequenceWidth) - 1;
            if (nextSeq > maxValue) {
                System.err.println("[ID GEN ERROR] Sequence exhausted for course: " + courseId);
                return null;
            }
            return courseId + String.format("%0" + sequenceWidth + "d", nextSeq);
        } catch (SQLException e) {
            System.err.println("[ID GEN ERROR] Failed calculating sequence metadata.");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Reads the numeric suffix after a course-id prefix (e.g. CS101001 → 1).
     * Seeded IDs such as 11001 do not use the CS101 prefix, so they are ignored
     * and do not collide with newly generated alphanumeric IDs.
     */
    private Integer parseSequenceSuffix(String questionId, String courseId) {
        if (questionId == null || courseId == null || !questionId.startsWith(courseId)
                || questionId.length() <= courseId.length()) {
            return null;
        }
        String suffix = questionId.substring(courseId.length());
        for (int i = 0; i < suffix.length(); i++) {
            if (!Character.isDigit(suffix.charAt(i))) {
                return null;
            }
        }
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final Object VERSION_SCHEMA_LOCK = new Object();
    private static volatile boolean versionColumnsReady = false;

    /**
     * Non-destructive live-DB migration: add lineage columns and backfill
     * existing rows as Version 1 / current without changing question ids.
     */
    public static void ensureVersionColumns(Connection conn) {
        if (conn == null) {
            return;
        }
        ensureImageColumns(conn);
        if (versionColumnsReady) {
            return;
        }
        synchronized (VERSION_SCHEMA_LOCK) {
            if (versionColumnsReady) {
                return;
            }
            try {
                addColumnIfMissing(conn, "questions", "root_question_id", "VARCHAR(10) NULL");
                addColumnIfMissing(conn, "questions", "version_number", "INT NOT NULL DEFAULT 1");
                addColumnIfMissing(conn, "questions", "is_latest", "TINYINT(1) NOT NULL DEFAULT 1");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                            "UPDATE questions SET root_question_id = question_id "
                                    + "WHERE root_question_id IS NULL OR root_question_id = ''");
                }
                if (!indexExists(conn, "questions", "uk_question_lineage")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(
                                "ALTER TABLE questions ADD UNIQUE KEY uk_question_lineage "
                                        + "(root_question_id, version_number)");
                    } catch (SQLException e) {
                        System.err.println("[QUESTION-SCHEMA] Unique lineage key: " + e.getMessage());
                    }
                }
                ensureImageColumns(conn);
                versionColumnsReady = true;
            } catch (SQLException e) {
                System.err.println("[QUESTION-SCHEMA] Could not ensure version columns: " + e.getMessage());
            }
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition)
            throws SQLException {
        if (columnExists(conn, table, column)) {
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet columns = meta.getColumns(conn.getCatalog(), null, table, column)) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet columns = meta.getColumns(conn.getCatalog(), null, table, column.toUpperCase())) {
            return columns.next();
        }
    }

    private static boolean indexExists(Connection conn, String table, String indexName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet indexes = meta.getIndexInfo(conn.getCatalog(), null, table, false, false)) {
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void bindIllustration(PreparedStatement stmt, int filenameIndex, int dataIndex,
                                         byte[] imageData, String imageFilename) throws SQLException {
        byte[] normalized = com.hsts.shared.model.QuestionIllustration.normalize(imageData);
        String storedName = com.hsts.shared.model.QuestionIllustration.filenameForStorage(normalized, imageFilename);
        if (normalized == null) {
            stmt.setNull(filenameIndex, Types.VARCHAR);
            stmt.setNull(dataIndex, Types.BLOB);
        } else {
            stmt.setString(filenameIndex, storedName);
            stmt.setBytes(dataIndex, normalized);
        }
    }

    private static final Object IMAGE_SCHEMA_LOCK = new Object();
    private static volatile boolean imageColumnsReady = false;

    /**
     * Non-destructive live-DB migration: add nullable illustration columns.
     * Existing questions keep NULL image data; question ids are unchanged.
     */
    public static void ensureImageColumns(Connection conn) {
        if (imageColumnsReady || conn == null) {
            return;
        }
        synchronized (IMAGE_SCHEMA_LOCK) {
            if (imageColumnsReady) {
                return;
            }
            try {
                addColumnIfMissing(conn, "questions", "image_filename", "VARCHAR(255) NULL");
                addColumnIfMissing(conn, "questions", "image_data", "LONGBLOB NULL");
                imageColumnsReady = true;
            } catch (SQLException e) {
                System.err.println("[QUESTION-SCHEMA] Could not ensure image columns: " + e.getMessage());
            }
        }
    }
}