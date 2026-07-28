package com.hsts.shared.model;

import java.io.Serializable;

/**
 * Lifecycle status of an Exam (SUC-2/3 build -> SUC-4 approval -> SUC-6 taking).
 * DRAFT: created by a teacher, not yet submitted for approval.
 * PENDING_APPROVAL: submitted, waiting on a SubjectCoordinator.
 * APPROVED: coordinator approved it; students can take it once scheduled.
 * REJECTED: coordinator rejected it; teacher must revise and resubmit.
 */
public enum ExamStatus implements Serializable {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    IN_PROGRESS,
    DONE
}
