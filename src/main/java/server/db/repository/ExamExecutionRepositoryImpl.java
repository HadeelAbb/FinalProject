package server.db.repository;

import com.hsts.shared.model.ExamExecution;
import server.db.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SUC 2.2 / SUC-4: persistence for exam executions - each row is one
 * "sitting" of an approved exam, with its own code and open/close window.
 */
public class ExamExecutionRepositoryImpl {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public boolean save(ExamExecution execution) {
        String sql = "INSERT INTO exam_executions (execution_id, exam_id, execution_code, " +
                "scheduled_start, scheduled_end, extra_minutes_granted, created_by_teacher_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, execution.getExecutionId());
            stmt.setString(2, execution.getExamId());
            stmt.setString(3, execution.getExecutionCode());
            stmt.setTimestamp(4, execution.getScheduledStart() != null ? Timestamp.valueOf(execution.getScheduledStart()) : null);
            stmt.setTimestamp(5, execution.getScheduledEnd() != null ? Timestamp.valueOf(execution.getScheduledEnd()) : null);
            stmt.setInt(6, execution.getExtraMinutesGranted());
            stmt.setString(7, execution.getCreatedByTeacherId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** SUC-17: adds minutes to this execution's running total - applies to anyone taking
     * this execution from now on, not just students already connected at this instant. */
    public boolean addExtraMinutes(String executionId, int additionalMinutes) {
        String sql = "UPDATE exam_executions SET extra_minutes_granted = extra_minutes_granted + ? WHERE execution_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, additionalMinutes);
            stmt.setString(2, executionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ExamExecution> findByExamId(String examId) {
        List<ExamExecution> results = new ArrayList<>();
        String sql = "SELECT * FROM exam_executions WHERE exam_id = ? ORDER BY scheduled_start DESC";
        Connection conn = dbManager.getConnection();
        if (conn == null) return results;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    /** Finds the still-usable execution matching this code for this exam. */
    public Optional<ExamExecution> findByExamIdAndCode(String examId, String code) {
        String sql = "SELECT * FROM exam_executions WHERE exam_id = ? AND LOWER(execution_code) = LOWER(?)";
        Connection conn = dbManager.getConnection();
        if (conn == null) return Optional.empty();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examId);
            stmt.setString(2, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /** Production student/code lookup: execution code is unique across exam_executions. */
    public Optional<ExamExecution> findByCode(String code) {
        String sql = "SELECT * FROM exam_executions WHERE LOWER(execution_code) = LOWER(?)";
        Connection conn = dbManager.getConnection();
        if (conn == null) return Optional.empty();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<ExamExecution> findById(String executionId) {
        String sql = "SELECT * FROM exam_executions WHERE execution_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return Optional.empty();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, executionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private ExamExecution mapRow(ResultSet rs) throws SQLException {
        ExamExecution execution = new ExamExecution();
        execution.setExecutionId(rs.getString("execution_id"));
        execution.setExamId(rs.getString("exam_id"));
        execution.setExecutionCode(rs.getString("execution_code"));
        Timestamp start = rs.getTimestamp("scheduled_start");
        if (start != null) execution.setScheduledStart(start.toLocalDateTime());
        Timestamp end = rs.getTimestamp("scheduled_end");
        if (end != null) execution.setScheduledEnd(end.toLocalDateTime());
        execution.setExtraMinutesGranted(rs.getInt("extra_minutes_granted"));
        execution.setCreatedByTeacherId(rs.getString("created_by_teacher_id"));
        return execution;
    }
}