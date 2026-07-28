package server.db.repository;

import com.hsts.shared.model.ExamAnswer;
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
        String sqlAnswer = "INSERT INTO exam_answers (exam_answer_id, exam_id, student_id, submitted_at, auto_submitted, auto_score, final_score, grade_confirmed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        String sqlSelected = "INSERT INTO student_selected_answers (exam_answer_id, question_id, selected_answer_text) VALUES (?, ?, ?)";

        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sqlAnswer)) {
                stmt.setString(1, answer.getExamAnswerId());
                stmt.setString(2, answer.getExamId());
                stmt.setString(3, answer.getStudentId());
                stmt.setTimestamp(4, answer.getSubmittedAt() != null ? Timestamp.valueOf(answer.getSubmittedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
                stmt.setBoolean(5, answer.isAutoSubmitted());
                stmt.setDouble(6, answer.getAutoScore() != null ? answer.getAutoScore() : 0.0);
                stmt.setDouble(7, answer.getFinalScore() != null ? answer.getFinalScore() : 0.0);
                stmt.setBoolean(8, answer.isGradeConfirmed());

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

        if (scores.isEmpty()) {
            return Optional.empty(); // No confirmed grades available yet
        }

        int total = scores.size();

        // 1. Calculate Mean
        double sum = 0;
        for (double s : scores) sum += s;
        double mean = Math.round((sum / total) * 100.0) / 100.0;

        // 2. Calculate Median
        double median;
        if (total % 2 == 1) {
            median = scores.get(total / 2);
        } else {
            median = (scores.get((total / 2) - 1) + scores.get(total / 2)) / 2.0;
        }
        median = Math.round(median * 100.0) / 100.0;

        // 3. Calculate Decile Distribution (0-9, 10-19, ..., 90-100)
        int[] deciles = new int[10];
        for (double score : scores) {
            int index = (int) (score / 10.0);
            if (index >= 10) index = 9; // Handle score == 100.0
            if (index < 0) index = 0;
            deciles[index]++;
        }

        return Optional.of(new ExamStats(examId, total, mean, median, deciles));
    }
}