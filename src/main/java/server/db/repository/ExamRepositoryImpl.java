package server.db.repository;

import com.hsts.shared.model.*;
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
        String sqlExam = "INSERT INTO exams (exam_id, course_id, title, instructions, duration_minutes, " +
                "status, created_by_teacher_id, execution_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlQuestions = "INSERT INTO exam_questions (exam_id, question_id, question_order) VALUES (?, ?, ?)";

        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sqlExam)) {
                stmt.setString(1, exam.getExamId());
                stmt.setString(2, exam.getCourseId());
                stmt.setString(3, exam.getTitle());
                stmt.setString(4, exam.getInstructionsForStudents());
                stmt.setInt(5, exam.getDurationMinutes());
                stmt.setString(6, exam.getStatus().name());
                stmt.setString(7, exam.getCreatedByTeacherId());
                stmt.setString(8, exam.getExecutionCode());
                stmt.executeUpdate();
            }

            try (PreparedStatement qStmt = conn.prepareStatement(sqlQuestions)) {
                int order = 1;
                for (Question q : exam.getQuestions()) {
                    qStmt.setString(1, exam.getExamId());
                    qStmt.setString(2, q.getQuestionId());
                    qStmt.setInt(3, order++);
                    qStmt.addBatch();
                }
                qStmt.executeBatch();
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
        exam.setDurationMinutes(rs.getInt("duration_minutes"));
        exam.setStatus(ExamStatus.valueOf(rs.getString("status")));
        exam.setCreatedByTeacherId(rs.getString("created_by_teacher_id"));
        exam.setApprovedByCoordinatorId(rs.getString("approved_by_coordinator_id"));
        exam.setRejectionReason(rs.getString("rejection_reason"));
        exam.setExecutionCode(rs.getString("execution_code"));

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
        String sql = "SELECT q.* FROM questions q " +
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
                    questions.add(q);
                }
            }
        }
        return questions;
    }
}