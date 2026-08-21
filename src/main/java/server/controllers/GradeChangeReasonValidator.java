package server.controllers;

/**
 * Server rule for CONFIRM_GRADE: a non-blank reason is required only when
 * the requested final score differs from the official stored autoScore.
 */
public final class GradeChangeReasonValidator {

    public static final String REASON_REQUIRED = "A reason is required when changing the automatic grade.";
    public static final String FINAL_SCORE_REQUIRED = "Final score is required.";

    private GradeChangeReasonValidator() {
    }

    /**
     * @param storedAutoScore official ExamAnswer.autoScore from storage, never a client claim
     * @return null if confirmation may proceed; otherwise a failure message
     */
    public static String validate(Double storedAutoScore, Double requestedFinalScore, String reason) {
        if (requestedFinalScore == null) {
            return FINAL_SCORE_REQUIRED;
        }
        if (!scoreChanged(storedAutoScore, requestedFinalScore)) {
            return null;
        }
        if (reason == null || reason.isBlank()) {
            return REASON_REQUIRED;
        }
        return null;
    }

    static boolean scoreChanged(Double storedAutoScore, Double requestedFinalScore) {
        if (storedAutoScore == null) {
            return true;
        }
        return Double.compare(storedAutoScore, requestedFinalScore) != 0;
    }
}
