package com.hsts.shared.model;

/**
 * Builder enable/disable rules for a selected exam. No JavaFX.
 */
public class ExamDraftActionsTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        Exam currentDraft = exam("E2", 2, true, ExamStatus.DRAFT);
        check(ExamDraftActions.canReview(currentDraft), "Current DRAFT: Review enabled");
        check(ExamDraftActions.canSubmitForApproval(currentDraft), "Current DRAFT: Submit enabled");
        check(ExamDraftActions.canSaveNewVersion(currentDraft), "Current DRAFT: Save as new version enabled");

        Exam currentApproved = exam("E1", 1, true, ExamStatus.APPROVED);
        check(ExamDraftActions.canReview(currentApproved), "Current APPROVED: Review enabled");
        check(!ExamDraftActions.canSubmitForApproval(currentApproved), "Current APPROVED: Submit disabled");
        check(ExamDraftActions.canSaveNewVersion(currentApproved), "Current APPROVED: Save as new version enabled");

        Exam currentPending = exam("E2", 2, true, ExamStatus.PENDING_APPROVAL);
        check(!ExamDraftActions.canSubmitForApproval(currentPending), "Current pending: Submit disabled");
        check(ExamDraftActions.canSaveNewVersion(currentPending), "Current pending: Save as new version still allowed");

        Exam currentRejected = exam("E2", 2, true, ExamStatus.REJECTED);
        check(ExamDraftActions.canSubmitForApproval(currentRejected),
                "Current REJECTED: Submit enabled (existing revise-and-resubmit path)");
        check(ExamDraftActions.canSaveNewVersion(currentRejected),
                "Current REJECTED: Save as new version enabled");

        Exam historicalDraft = exam("E-OLD", 1, false, ExamStatus.DRAFT);
        check(ExamDraftActions.canReview(historicalDraft), "Historical DRAFT: Review enabled (read-only view)");
        check(!ExamDraftActions.canSubmitForApproval(historicalDraft), "Historical DRAFT: Submit disabled");
        check(!ExamDraftActions.canSaveNewVersion(historicalDraft), "Historical DRAFT: Save as new version disabled");

        Exam historicalApproved = exam("E1", 1, false, ExamStatus.APPROVED);
        check(!ExamDraftActions.canSubmitForApproval(historicalApproved), "Historical APPROVED: Submit disabled");
        check(!ExamDraftActions.canSaveNewVersion(historicalApproved), "Historical APPROVED: Save as new version disabled");

        check(!ExamDraftActions.canReview(null), "no selection: Review disabled");
        check(!ExamDraftActions.canSubmitForApproval(null), "no selection: Submit disabled");
        check(!ExamDraftActions.canSaveNewVersion(null), "no selection: Save as new version disabled");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static Exam exam(String id, int version, boolean latest, ExamStatus status) {
        Exam exam = new Exam(id, "11", "Title", "instructions", java.util.List.of(), 60, "T1");
        exam.setRootExamId("E1");
        exam.setVersionNumber(version);
        exam.setLatest(latest);
        exam.setStatus(status);
        return exam;
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  OK  - " + description);
        } else {
            System.out.println("FAIL  - " + description);
            failCount++;
        }
    }
}
