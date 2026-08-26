//to manage CRUD operations for course_bot_configs

package server.db.repository;

import com.hsts.shared.model.CourseBotConfig;
import server.db.DatabaseManager;

import java.sql.*;
import java.util.Optional;

public class BotConfigRepositoryImpl {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public Optional<CourseBotConfig> findByCourseId(String courseId) {
        String sql = "SELECT * FROM course_bot_configs WHERE course_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return Optional.empty();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    CourseBotConfig config = new CourseBotConfig();
                    config.setCourseId(rs.getString("course_id"));
                    config.setBotName(rs.getString("bot_name"));
                    config.setKnowledgeSources(rs.getString("knowledge_sources"));
                    config.setActive(rs.getBoolean("is_active"));
                    config.setLastUpdatedBy(rs.getString("last_updated_by"));
                    Timestamp ts = rs.getTimestamp("last_updated_at");
                    if (ts != null) {
                        config.setLastUpdatedAt(ts.toLocalDateTime());
                    }
                    return Optional.of(config);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean saveOrUpdate(CourseBotConfig config) {
        String sql = "INSERT INTO course_bot_configs (course_id, bot_name, knowledge_sources, is_active, last_updated_by) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "bot_name = VALUES(bot_name), " +
                "knowledge_sources = VALUES(knowledge_sources), " +
                "is_active = VALUES(is_active), " +
                "last_updated_by = VALUES(last_updated_by)";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, config.getCourseId());
            stmt.setString(2, config.getBotName());
            stmt.setString(3, config.getKnowledgeSources());
            stmt.setBoolean(4, config.isActive());
            stmt.setString(5, config.getLastUpdatedBy());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}