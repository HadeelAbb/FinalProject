package server.db.repository;

import com.hsts.shared.model.BotInteraction;
import server.db.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BotRepositoryImpl {

    public void save(BotInteraction interaction) throws SQLException {
        String sql = "INSERT INTO bot_interactions (interaction_id, student_id, course_id, user_question, bot_response) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, interaction.getInteractionId());
            stmt.setString(2, interaction.getStudentId());
            stmt.setString(3, interaction.getCourseId());
            stmt.setString(4, interaction.getQuestion()); // Matches getQuestion() in BotInteraction
            stmt.setString(5, interaction.getAnswer());   // Matches getAnswer() in BotInteraction
            stmt.executeUpdate();
        }
    }

    public List<BotInteraction> findByStudentId(String studentId) throws SQLException {
        List<BotInteraction> history = new ArrayList<>();
        String sql = "SELECT * FROM bot_interactions WHERE student_id = ? ORDER BY timestamp DESC";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BotInteraction interaction = new BotInteraction(
                            rs.getString("interaction_id"),
                            rs.getString("student_id"),
                            rs.getString("course_id"),
                            rs.getString("user_question"),
                            rs.getString("bot_response")
                    );
                    history.add(interaction);
                }
            }
        }
        return history;
    }

    /** All interactions across all students/courses - used to compute usage stats (SUC-13). */
    public List<BotInteraction> findAll() throws SQLException {
        List<BotInteraction> all = new ArrayList<>();
        String sql = "SELECT * FROM bot_interactions";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    all.add(new BotInteraction(
                            rs.getString("interaction_id"),
                            rs.getString("student_id"),
                            rs.getString("course_id"),
                            rs.getString("user_question"),
                            rs.getString("bot_response")
                    ));
                }
            }
        }
        return all;
    }
}