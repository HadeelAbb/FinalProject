package server.controllers;

import com.hsts.shared.model.ExamStatus;

import java.time.LocalDateTime;

/**
 * Server-side rules for opening an ExamExecution. The teacher GUI already
 * filters to approved exams and checks the time window, but a forged OCSF
 * request can skip that, so the server must reject invalid creates.
 */
public final class ExamExecutionCreateValidator {

    public static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private ExamExecutionCreateValidator() {
    }

    public static String validateApproved(ExamStatus status) {
        if (status != ExamStatus.APPROVED) {
            return "Only approved exams can be executed.";
        }
        return null;
    }

    public static String validateWindow(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "Opening and closing times are required.";
        }
        if (!end.isAfter(start)) {
            return "Closing time must be after opening time.";
        }
        return null;
    }

    public static String normalizeExecutionCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    public static String validateExecutionCode(String code) {
        String trimmed = normalizeExecutionCode(code);
        if (trimmed == null || trimmed.length() != 4) {
            return "Execution code must contain exactly 4 characters.";
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (CODE_ALPHABET.indexOf(trimmed.charAt(i)) < 0) {
                return "Execution code may contain only A-Z and 0-9.";
            }
        }
        return null;
    }
}
