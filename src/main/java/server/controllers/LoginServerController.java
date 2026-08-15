package server.controllers;

import server.db.DatabaseManager; // This matches your exact database package!
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Traffic cop handling user authentication sessions and duplicate-login protections.
 */
public class LoginServerController {

    private final Set<String> activeLoggedInUsers;
    private final DatabaseManager dbManager;

    public LoginServerController() {
        this.activeLoggedInUsers = new HashSet<>();
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Authenticates a user against the database records securely using PreparedStatement compilation.
     */
    public boolean login(String username, String password) {
        if (isAlreadyLoggedIn(username)) {
            System.out.println("[LOGIN] Denied: " + username + " is already active in a session.");
            return false;
        }

        String sql = "SELECT password FROM users WHERE username = ?";
        Connection conn = dbManager.getConnection();

        if (conn == null) {
            System.err.println("[LOGIN ERROR] No active database connection pipeline!");
            return false;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    if (storedPassword.equals(password)) {
                        activeLoggedInUsers.add(username);
                        System.out.println("[LOGIN] Success: User '" + username + "' logged in.");
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[LOGIN ERROR] Exception occurred during authentication compiling.");
            e.printStackTrace();
        }

        System.out.println("[LOGIN] Denied: Invalid username or password for '" + username + "'.");
        return false;
    }

    public void logout(String username) {
        if (activeLoggedInUsers.remove(username)) {
            System.out.println("[LOGOUT] Success: User '" + username + "' logged out safely.");
        } else {
            System.out.println("[LOGOUT] Warning: '" + username + "' wasn't flagged as logged in.");
        }
    }

    public boolean isAlreadyLoggedIn(String username) {
        return activeLoggedInUsers.contains(username);
    }

    /** Called when the server is shutting down - clears every active session so no
     * account is left stuck as "logged in" for the next time the server starts. */
    public void logoutAll() {
        int count = activeLoggedInUsers.size();
        activeLoggedInUsers.clear();
        System.out.println("[SHUTDOWN] Cleared " + count + " active session(s).");
    }
}