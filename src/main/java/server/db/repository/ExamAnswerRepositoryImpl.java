package server.db.repository;

import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.ExamStatisticsCalculator;
import com.hsts.shared.model.ExamStats;
import server.db.DatabaseManager;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public class ExamAnswerRepositoryImpl {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public boolean save(ExamAnswer answer) {
        String sqlAnswer = "INSERT INTO exam_answers (exam_answer_id, exam_id, execution_id, student_id, submitted_at, auto_submitted, auto_score, final_score, grade_confirmed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String sqlSelected = "INSERT INTO student_selected_answers (exam_answer_id, question_id, selected_answer_text) VALUES (?, ?, ?)";

        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sqlAnswer)) {
                stmt.setString(1, answer.getExamAnswerId());
                stmt.setString(2, answer.getExamId());
                stmt.setString(3, answer.getExecutionId());
                stmt.setString(4, answer.getStudentId());
                stmt.setTimestamp(5, answer.getSubmittedAt() != null ? Timestamp.valueOf(answer.getSubmittedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
                stmt.setBoolean(6, answer.isAutoSubmitted());
                stmt.setDouble(7, answer.getAutoScore() != null ? answer.getAutoScore() : 0.0);
                stmt.setDouble(8, answer.getFinalScore() != null ? answer.getFinalScore() : 0.0);
                stmt.setBoolean(9, answer.isGradeConfirmed());

                stmt.executeUpdate();
            }

            if (answer.getSelectedAnswers() != null && !answer.getSelectedAnswers().isEmpty()) {
                try (PreparedStatement stmtSelected = conn.prepareStatement(sqlSelected)) {
                    for (Map.Entry<String, String> entry : answer.getSelectedAnswers().entrySet()) {
                        stmtSelected.setString(1, answer.getExamAnswerId());
                        stmtSelected.setString(2, entry.getKey());
                        stmtSelected.setString(3, entry.getValue());
                        stmtSelected.addBatch();
                    }
                    stmtSelected.executeBatch();
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            if (e.getErrorCode() == 1062) {
                System.err.println("Database rejected insert: Student has already submitted this exam or duplicate ID.");
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    public Optional<ExamAnswer> findById(String examAnswerId) {
        String sqlAnswer = "SELECT * FROM exam_answers WHERE exam_answer_id = ?";
        String sqlSelected = "SELECT question_id, selected_answer_text FROM student_selected_answers WHERE exam_answer_id = ?";

        Connection conn = dbManager.getConnection();
        if (conn == null) return Optional.empty();

        try (PreparedStatement stmt = conn.prepareStatement(sqlAnswer)) {
            stmt.setString(1, examAnswerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ExamAnswer answer = new ExamAnswer(
                            rs.getString("exam_answer_id"),
                            rs.getString("exam_id"),
                            rs.getString("student_id")
                    );
                    answer.setExecutionId(rs.getString("execution_id"));

                    Timestamp submittedAt = rs.getTimestamp("submitted_at");
                    if (submittedAt != null) {
                        answer.setSubmittedAt(submittedAt.toLocalDateTime());
                    }

                    answer.setAutoSubmitted(rs.getBoolean("auto_submitted"));
                    answer.setAutoScore(rs.getDouble("auto_score"));
                    answer.setFinalScore(rs.getDouble("final_score"));
                    answer.setTeacherComment(rs.getString("teacher_comment"));
                    answer.setGradeConfirmed(rs.getBoolean("grade_confirmed"));

                    Map<String, String> selectedAnswers = new HashMap<>();
                    try (PreparedStatement stmtSelected = conn.prepareStatement(sqlSelected)) {
                        stmtSelected.setString(1, examAnswerId);
                        try (ResultSet rsSelected = stmtSelected.executeQuery()) {
                            while (rsSelected.next()) {
                                selectedAnswers.put(
                                        rsSelected.getString("question_id"),
                                        rsSelected.getString("selected_answer_text")
                                );
                            }
                        }
                    }
                    answer.setSelectedAnswers(selectedAnswers);

                    return Optional.of(answer);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean update(ExamAnswer answer) {
        String sql = "UPDATE exam_answers SET final_score = ?, teacher_comment = ?, grade_confirmed = ? WHERE exam_answer_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, answer.getFinalScore());
            stmt.setString(2, answer.getTeacherComment());
            stmt.setBoolean(3, answer.isGradeConfirmed());
            stmt.setString(4, answer.getExamAnswerId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteById(String examAnswerId) {
        String sql = "DELETE FROM exam_answers WHERE exam_answer_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examAnswerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Chunk 2: Computes Mean, Median, and Decile Distribution for confirmed scores of an exam.
     */
    public Optional<ExamStats> getExamStats(String examId) {
        String sql = "SELECT final_score FROM exam_answers WHERE exam_id = ? AND grade_confirmed = 1 ORDER BY final_score ASC";
        Connection conn = dbManager.getConnection();
        if (conn == null) return Optional.empty();

        List<Double> scores = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    scores.add(rs.getDouble("final_score"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        ExamStats stats = ExamStatisticsCalculator.toExamStats(examId, scores);
        return stats != null ? Optional.of(stats) : Optional.empty();
    }

    /** All answers a student has submitted (used for "already taken" checks and SUC-10 results). */
    public List<ExamAnswer> findByStudentId(String studentId) {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT exam_answer_id FROM exam_answers WHERE student_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("exam_answer_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        List<ExamAnswer> results = new ArrayList<>();
        for (String id : ids) {
            findById(id).ifPresent(results::add);
        }
        return results;
    }

    /**
     * Submitted-but-not-yet-confirmed answers for exams a given teacher created
     * (SUC-9: teacher's grading queue).
     */
    public List<ExamAnswer> findPendingGradingForTeacher(String teacherId) {
        String sql = "SELECT ea.exam_answer_id FROM exam_answers ea " +
                "JOIN exams e ON ea.exam_id = e.exam_id " +
                "WHERE e.created_by_teacher_id = ? AND ea.grade_confirmed = 0 AND ea.submitted_at IS NOT NULL";
        List<String> ids = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        if (conn == null) return new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, teacherId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("exam_answer_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        List<ExamAnswer> results = new ArrayList<>();
        for (String id : ids) {
            findById(id).ifPresent(results::add);
        }
        return results;
    }

    /**
     * All submitted attempts for one exam (pending and confirmed).
     * In-progress sittings that were never submitted are not included.
     */
    public List<ExamAnswer> findSubmittedByExamId(String examId) {
        String sql = "SELECT exam_answer_id FROM exam_answers WHERE exam_id = ? AND submitted_at IS NOT NULL "
                + "ORDER BY submitted_at ASC";
        List<String> ids = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        if (conn == null) return new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("exam_answer_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        List<ExamAnswer> results = new ArrayList<>();
        for (String id : ids) {
            findById(id).ifPresent(results::add);
        }
        return results;
    }

    /**
     * Section 4: since this client always auto-submits at time-up, every student
     * who starts an execution eventually gets a row here - so "started" is well
     * approximated by "total submissions" for that execution, with no extra
     * tracking needed. Splits those into finished-themselves vs ran-out-of-time.
     */
    public com.hsts.shared.model.ExecutionStats getExecutionStats(String executionId) {
        String sql = "SELECT " +
                "COUNT(*) AS total, " +
                "SUM(CASE WHEN auto_submitted = 0 THEN 1 ELSE 0 END) AS finished, " +
                "SUM(CASE WHEN auto_submitted = 1 THEN 1 ELSE 0 END) AS timedout " +
                "FROM exam_answers WHERE execution_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return new com.hsts.shared.model.ExecutionStats(executionId, 0, 0, 0);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, executionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new com.hsts.shared.model.ExecutionStats(executionId,
                            rs.getInt("total"), rs.getInt("finished"), rs.getInt("timedout"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new com.hsts.shared.model.ExecutionStats(executionId, 0, 0, 0);
    }

    /** All confirmed results across every student and exam - for the Principal's read-only view (SUC 7.3.1). */
    public List<ExamAnswer> findAllConfirmed() {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT exam_answer_id FROM exam_answers WHERE grade_confirmed = 1";
        Connection conn = dbManager.getConnection();
        if (conn == null) return new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("exam_answer_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        List<ExamAnswer> results = new ArrayList<>();
        for (String id : ids) {
            findById(id).ifPresent(results::add);
        }
        return results;
    }
}