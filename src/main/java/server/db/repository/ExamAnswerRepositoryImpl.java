package server.db.repository;

import com.hsts.shared.model.ExamAnswer;
import server.db.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExamAnswerRepositoryImpl implements Repository<ExamAnswer, String> {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    @Override
    public Optional<ExamAnswer> findById(String examAnswerId) {
        String sql = "SELECT * FROM exam_answers WHERE exam_answer_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return Optional.empty();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examAnswerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ExamAnswer answer = mapResultSetToExamAnswer(rs);
                    answer.setSelectedAnswers(findSelectedAnswers(examAnswerId, conn));
                    return Optional.of(answer);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<ExamAnswer> findAll() {
        List<ExamAnswer> answers = new ArrayList<>();
        String sql = "SELECT * FROM exam_answers";
        Connection conn = dbManager.getConnection();
        if (conn == null) return answers;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ExamAnswer answer = mapResultSetToExamAnswer(rs);
                answer.setSelectedAnswers(findSelectedAnswers(answer.getExamAnswerId(), conn));
                answers.add(answer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return answers;
    }

    @Override
    public boolean save(ExamAnswer answer) {
        String sqlAnswer = "INSERT INTO exam_answers (exam_answer_id, exam_id, student_id, " +
                "started_at, submitted_at, auto_submitted, auto_score, final_score, teacher_comment, " +
                "grade_confirmed, extra_minutes_granted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String sqlSelections = "INSERT INTO student_selected_answers (exam_answer_id, question_id, " +
                "selected_answer_text) VALUES (?, ?, ?)";

        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            // 1. Insert main submission row
            try (PreparedStatement stmt = conn.prepareStatement(sqlAnswer)) {
                stmt.setString(1, answer.getExamAnswerId());
                stmt.setString(2, answer.getExamId());
                stmt.setString(3, answer.getStudentId());
                stmt.setTimestamp(4, answer.getStartedAt() != null ? Timestamp.valueOf(answer.getStartedAt()) : null);
                stmt.setTimestamp(5, answer.getSubmittedAt() != null ? Timestamp.valueOf(answer.getSubmittedAt()) : null);
                stmt.setBoolean(6, answer.isAutoSubmitted());
                stmt.setObject(7, answer.getAutoScore());
                stmt.setObject(8, answer.getFinalScore());
                stmt.setString(9, answer.getTeacherComment());
                stmt.setBoolean(10, answer.isGradeConfirmed());
                stmt.setInt(11, answer.getExtraMinutesGranted());
                stmt.executeUpdate();
            }

            // 2. Batch insert student's chosen choices
            try (PreparedStatement selStmt = conn.prepareStatement(sqlSelections)) {
                for (Map.Entry<String, String> entry : answer.getSelectedAnswers().entrySet()) {
                    selStmt.setString(1, answer.getExamAnswerId());
                    selStmt.setString(2, entry.getKey());
                    selStmt.setString(3, entry.getValue());
                    selStmt.addBatch();
                }
                selStmt.executeBatch();
            }

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

    @Override
    public boolean update(ExamAnswer answer) {
        String sql = "UPDATE exam_answers SET submitted_at = ?, auto_submitted = ?, auto_score = ?, " +
                "final_score = ?, teacher_comment = ?, grade_confirmed = ?, extra_minutes_granted = ? " +
                "WHERE exam_answer_id = ?";

        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, answer.getSubmittedAt() != null ? Timestamp.valueOf(answer.getSubmittedAt()) : null);
            stmt.setBoolean(2, answer.isAutoSubmitted());
            stmt.setObject(3, answer.getAutoScore());
            stmt.setObject(4, answer.getFinalScore());
            stmt.setString(5, answer.getTeacherComment());
            stmt.setBoolean(6, answer.isGradeConfirmed());
            stmt.setInt(7, answer.getExtraMinutesGranted());
            stmt.setString(8, answer.getExamAnswerId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
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

    private ExamAnswer mapResultSetToExamAnswer(ResultSet rs) throws SQLException {
        ExamAnswer answer = new ExamAnswer();
        answer.setExamAnswerId(rs.getString("exam_answer_id"));
        answer.setExamId(rs.getString("exam_id"));
        answer.setStudentId(rs.getString("student_id"));

        Timestamp started = rs.getTimestamp("started_at");
        if (started != null) answer.setStartedAt(started.toLocalDateTime());

        Timestamp submitted = rs.getTimestamp("submitted_at");
        if (submitted != null) answer.setSubmittedAt(submitted.toLocalDateTime());

        answer.setAutoSubmitted(rs.getBoolean("auto_submitted"));
        answer.setAutoScore((Double) rs.getObject("auto_score"));
        answer.setFinalScore((Double) rs.getObject("final_score"));
        answer.setTeacherComment(rs.getString("teacher_comment"));
        answer.setGradeConfirmed(rs.getBoolean("grade_confirmed"));
        answer.setExtraMinutesGranted(rs.getInt("extra_minutes_granted"));
        return answer;
    }

    private Map<String, String> findSelectedAnswers(String examAnswerId, Connection conn) throws SQLException {
        Map<String, String> selections = new HashMap<>();
        String sql = "SELECT question_id, selected_answer_text FROM student_selected_answers WHERE exam_answer_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examAnswerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    selections.put(rs.getString("question_id"), rs.getString("selected_answer_text"));
                }
            }
        }
        return selections;
    }
}