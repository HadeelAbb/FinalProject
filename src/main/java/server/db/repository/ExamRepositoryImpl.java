package server.db.repository;

import com.hsts.shared.model.*;
import server.controllers.QuestionServerController;
import server.db.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ExamRepositoryImpl implements Repository<Exam, String> {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    @Override
    public Optional<Exam> findById(String examId) {
        String sql = "SELECT * FROM exams WHERE exam_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return Optional.empty();
        ensurePointsColumn(conn);
        ensureVersionColumns(conn);
        QuestionServerController.ensureVersionColumns(conn);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Exam exam = mapResultSetToExam(rs);
                    exam.setQuestions(findQuestionsForExam(examId, conn));
                    return Optional.of(exam);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Exam> findAll() {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT * FROM exams";
        Connection conn = dbManager.getConnection();
        if (conn == null) return exams;
        ensurePointsColumn(conn);
        ensureVersionColumns(conn);
        QuestionServerController.ensureVersionColumns(conn);

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Exam exam = mapResultSetToExam(rs);
                exam.setQuestions(findQuestionsForExam(exam.getExamId(), conn));
                exams.add(exam);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exams;
    }

    @Override
    public boolean save(Exam exam) {
        String sqlExam = "INSERT INTO exams (exam_id, course_id, title, instructions, instructions_for_teacher, " +
                "duration_minutes, status, created_by_teacher_id, execution_code, " +
                "root_exam_id, version_number, is_latest) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = dbManager.getConnection();
        if (conn == null) return false;
        ensurePointsColumn(conn);
        ensureVersionColumns(conn);
        QuestionServerController.ensureVersionColumns(conn);

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sqlExam)) {
                bindExamInsert(stmt, exam);
                stmt.executeUpdate();
            }

            insertExamQuestions(conn, exam);

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackEx) { rollbackEx.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /**
     * Inserts a new physical exam version and demotes the previous latest row
     * in one transaction. Returns null on success, otherwise a failure message.
     * Does not UPDATE the source exam's content.
     */
    public String saveAsNewVersion(Exam newExam, String sourceExamId) {
        Connection conn = dbManager.getConnection();
        if (conn == null) {
            return "Database connection is unavailable.";
        }
        ensurePointsColumn(conn);
        ensureVersionColumns(conn);
        QuestionServerController.ensureVersionColumns(conn);

        try {
            conn.setAutoCommit(false);

            String rootId;
            boolean sourceIsLatest;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT exam_id, root_exam_id, is_latest FROM exams WHERE exam_id = ? FOR UPDATE")) {
                stmt.setString(1, sourceExamId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return server.controllers.ExamVersioning.SOURCE_NOT_FOUND;
                    }
                    String storedRoot = rs.getString("root_exam_id");
                    rootId = storedRoot != null && !storedRoot.isBlank() ? storedRoot : sourceExamId;
                    sourceIsLatest = rs.getInt("is_latest") == 1;
                }
            }

            if (!sourceIsLatest) {
                conn.rollback();
                return server.controllers.ExamVersioning.HISTORICAL_NOT_EDITABLE;
            }

            int maxVersion = 1;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT MAX(version_number) FROM exams WHERE root_exam_id = ? FOR UPDATE")) {
                stmt.setString(1, rootId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        maxVersion = rs.getInt(1);
                    }
                }
            }
            int nextVersion = server.controllers.ExamVersioning.nextVersionNumber(maxVersion);
            newExam.setRootExamId(rootId);
            newExam.setVersionNumber(nextVersion);
            newExam.setLatest(true);

            String sqlExam = "INSERT INTO exams (exam_id, course_id, title, instructions, instructions_for_teacher, " +
                    "duration_minutes, status, created_by_teacher_id, execution_code, " +
                    "root_exam_id, version_number, is_latest) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlExam)) {
                bindExamInsert(stmt, newExam);
                stmt.executeUpdate();
            }

            insertExamQuestions(conn, newExam);

            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE exams SET is_latest = 0 WHERE root_exam_id = ? AND exam_id <> ?")) {
                stmt.setString(1, rootId);
                stmt.setString(2, newExam.getExamId());
                stmt.executeUpdate();
            }

            conn.commit();
            return null;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rollbackEx) { rollbackEx.printStackTrace(); }
            e.printStackTrace();
            return "Failed to create exam version.";
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private void bindExamInsert(PreparedStatement stmt, Exam exam) throws SQLException {
        stmt.setString(1, exam.getExamId());
        stmt.setString(2, exam.getCourseId());
        stmt.setString(3, exam.getTitle());
        stmt.setString(4, exam.getInstructionsForStudents());
        stmt.setString(5, exam.getInstructionsForTeacher());
        stmt.setInt(6, exam.getDurationMinutes());
        stmt.setString(7, exam.getStatus().name());
        stmt.setString(8, exam.getCreatedByTeacherId());
        stmt.setString(9, exam.getExecutionCode());
        stmt.setString(10, exam.getRootExamId());
        stmt.setInt(11, exam.getVersionNumber());
        stmt.setInt(12, exam.isLatest() ? 1 : 0);
    }

    private void insertExamQuestions(Connection conn, Exam exam) throws SQLException {
        String sqlQuestions = "INSERT INTO exam_questions (exam_id, question_id, question_order, points) VALUES (?, ?, ?, ?)";
        try (PreparedStatement qStmt = conn.prepareStatement(sqlQuestions)) {
            int order = 1;
            for (Question q : exam.getQuestions()) {
                qStmt.setString(1, exam.getExamId());
                qStmt.setString(2, q.getQuestionId());
                qStmt.setInt(3, order++);
                qStmt.setInt(4, q.getPoints());
                qStmt.addBatch();
            }
            qStmt.executeBatch();
        }
    }

    @Override
    public boolean update(Exam exam) {
        String sql = "UPDATE exams SET status = ?, approved_by_coordinator_id = ?, rejection_reason = ?, " +
                "duration_minutes = ?, scheduled_start = ?, scheduled_end = ?, execution_code = ? WHERE exam_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, exam.getStatus().name());
            stmt.setString(2, exam.getApprovedByCoordinatorId());
            stmt.setString(3, exam.getRejectionReason());
            stmt.setInt(4, exam.getDurationMinutes());
            stmt.setTimestamp(5, exam.getScheduledStart() != null ? Timestamp.valueOf(exam.getScheduledStart()) : null);
            stmt.setTimestamp(6, exam.getScheduledEnd() != null ? Timestamp.valueOf(exam.getScheduledEnd()) : null);
            stmt.setString(7, exam.getExecutionCode());
            stmt.setString(8, exam.getExamId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteById(String examId) {
        String sql = "DELETE FROM exams WHERE exam_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Exam mapResultSetToExam(ResultSet rs) throws SQLException {
        Exam exam = new Exam();
        exam.setExamId(rs.getString("exam_id"));
        exam.setCourseId(rs.getString("course_id"));
        exam.setTitle(rs.getString("title"));
        exam.setInstructionsForStudents(rs.getString("instructions"));
        exam.setInstructionsForTeacher(rs.getString("instructions_for_teacher"));
        exam.setDurationMinutes(rs.getInt("duration_minutes"));
        exam.setStatus(ExamStatus.valueOf(rs.getString("status")));
        exam.setCreatedByTeacherId(rs.getString("created_by_teacher_id"));
        exam.setApprovedByCoordinatorId(rs.getString("approved_by_coordinator_id"));
        exam.setRejectionReason(rs.getString("rejection_reason"));
        exam.setExecutionCode(rs.getString("execution_code"));
        String rootExamId = rs.getString("root_exam_id");
        exam.setRootExamId(rootExamId != null && !rootExamId.isBlank() ? rootExamId : exam.getExamId());
        int versionNumber = rs.getInt("version_number");
        exam.setVersionNumber(rs.wasNull() || versionNumber < 1 ? 1 : versionNumber);
        exam.setLatest(rs.getInt("is_latest") == 1);

        Timestamp startTs = rs.getTimestamp("scheduled_start");
        if (startTs != null) {
            exam.setScheduledStart(startTs.toLocalDateTime());
        }

        Timestamp endTs = rs.getTimestamp("scheduled_end");
        if (endTs != null) {
            exam.setScheduledEnd(endTs.toLocalDateTime());
        }

        return exam;
    }

    private List<Question> findQuestionsForExam(String examId, Connection conn) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT q.*, eq.points AS exam_points FROM questions q " +
                "JOIN exam_questions eq ON q.question_id = eq.question_id " +
                "WHERE eq.exam_id = ? ORDER BY eq.question_order ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question();
                    q.setQuestionId(rs.getString("question_id"));
                    q.setText(rs.getString("text"));
                    q.setDifficulty(Difficulty.valueOf(rs.getString("difficulty")));
                    q.setInstructions(rs.getString("instructions"));
                    q.setTopic(rs.getString("topic"));
                    q.setCourseId(rs.getString("course_id"));
                    q.setPoints(rs.getInt("exam_points"));
                    String rootQuestionId = rs.getString("root_question_id");
                    q.setRootQuestionId(rootQuestionId);
                    int qVersion = rs.getInt("version_number");
                    q.setVersionNumber(rs.wasNull() || qVersion < 1 ? 1 : qVersion);
                    q.setLatest(rs.getInt("is_latest") == 1);
                    q.setAnswers(findAnswersForQuestion(q.getQuestionId(), conn));
                    QuestionIllustration.apply(q, rs.getBytes("image_data"), rs.getString("image_filename"));
                    questions.add(q);
                }
            }
        }
        return questions;
    }

    /** Loads the answer choices for one question - without this, exam-taking shows no options at all. */
    private List<QuestionAnswer> findAnswersForQuestion(String questionId, Connection conn) throws SQLException {
        List<QuestionAnswer> answers = new ArrayList<>();
        String sql = "SELECT answer_text, is_correct FROM question_answers WHERE question_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, questionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    answers.add(new QuestionAnswer(rs.getString("answer_text"), rs.getBoolean("is_correct")));
                }
            }
        }
        return answers;
    }

    private final Object pointsColumnLock = new Object();
    private volatile boolean pointsColumnReady = false;

    /** Non-destructive: add exam_questions.points if this live database predates Item 8. */
    private void ensurePointsColumn(Connection conn) {
        if (pointsColumnReady || conn == null) {
            return;
        }
        synchronized (pointsColumnLock) {
            if (pointsColumnReady) {
                return;
            }
            try {
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet columns = meta.getColumns(conn.getCatalog(), null, "exam_questions", "points")) {
                    if (!columns.next()) {
                        try (Statement alter = conn.createStatement()) {
                            alter.executeUpdate(
                                    "ALTER TABLE exam_questions ADD COLUMN points INT NOT NULL DEFAULT 20");
                        }
                    }
                }
                pointsColumnReady = true;
            } catch (SQLException e) {
                System.err.println("[EXAM-REPO] Could not ensure exam_questions.points: " + e.getMessage());
            }
        }
    }

    private final Object versionColumnLock = new Object();
    private volatile boolean versionColumnsReady = false;

    /**
     * Non-destructive live-DB migration: add exam lineage columns and backfill
     * existing rows as Version 1 / current without changing exam ids.
     */
    public void ensureVersionColumns(Connection conn) {
        if (versionColumnsReady || conn == null) {
            return;
        }
        synchronized (versionColumnLock) {
            if (versionColumnsReady) {
                return;
            }
            try {
                addColumnIfMissing(conn, "exams", "root_exam_id", "VARCHAR(10) NULL");
                addColumnIfMissing(conn, "exams", "version_number", "INT NOT NULL DEFAULT 1");
                addColumnIfMissing(conn, "exams", "is_latest", "TINYINT(1) NOT NULL DEFAULT 1");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                            "UPDATE exams SET root_exam_id = exam_id "
                                    + "WHERE root_exam_id IS NULL OR root_exam_id = ''");
                }
                if (!indexExists(conn, "exams", "uk_exam_lineage")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(
                                "ALTER TABLE exams ADD UNIQUE KEY uk_exam_lineage "
                                        + "(root_exam_id, version_number)");
                    } catch (SQLException e) {
                        System.err.println("[EXAM-SCHEMA] Unique lineage key: " + e.getMessage());
                    }
                }
                versionColumnsReady = true;
            } catch (SQLException e) {
                System.err.println("[EXAM-SCHEMA] Could not ensure exam version columns: " + e.getMessage());
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
}