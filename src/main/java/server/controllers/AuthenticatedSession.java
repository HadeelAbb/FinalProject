package server.controllers;

/**
 * Server-side identity for the current OCSF connection after LOGIN.
 * Role is taken from the authenticated user profile, never from a DTO.
 */
public final class AuthenticatedSession {

    public static final String STUDENT = "STUDENT";
    public static final String TEACHER = "TEACHER";
    public static final String COORDINATOR = "COORDINATOR";
    public static final String PRINCIPAL = "PRINCIPAL";

    private final String userId;
    private final String role;

    public AuthenticatedSession(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public boolean hasRole(String... allowedRoles) {
        if (allowedRoles == null) {
            return false;
        }
        for (String allowed : allowedRoles) {
            if (roleMatches(allowed)) {
                return true;
            }
        }
        return false;
    }

    private boolean roleMatches(String expected) {
        if (expected == null || role == null) {
            return false;
        }
        if (COORDINATOR.equalsIgnoreCase(expected)) {
            return "COORDINATOR".equalsIgnoreCase(role)
                    || "SUBJECT_COORDINATOR".equalsIgnoreCase(role);
        }
        return expected.equalsIgnoreCase(role);
    }
}
