package com.hsts.shared.model;

/**
 * Builder button/state rules for a selected exam. No JavaFX.
 * Submit uses the existing physical exam; it does not create a version.
 */
public final class ExamDraftActions {

    private ExamDraftActions() {
    }

    /** Review shows the selected exam's persisted definition. */
    public static boolean canReview(Exam exam) {
        return exam != null;
    }

    /**
     * Only the current DRAFT (or current REJECTED, which the existing
     * approval path already allows) can be submitted. Historical rows cannot.
     */
    public static boolean canSubmitForApproval(Exam exam) {
        if (exam == null || !exam.isLatest()) {
            return false;
        }
        ExamStatus status = exam.getStatus();
        return status == ExamStatus.DRAFT || status == ExamStatus.REJECTED;
    }

    public static boolean canSaveNewVersion(Exam exam) {
        return exam != null && exam.isLatest();
    }
}
