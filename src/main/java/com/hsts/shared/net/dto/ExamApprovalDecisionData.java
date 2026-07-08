package com.hsts.shared.net.dto;

import java.io.Serializable;

/**
 * Shared by both APPROVE_EXAM and REJECT_EXAM (SUC-4). reason is required
 * for a rejection and ignored for an approval.
 */
public class ExamApprovalDecisionData implements Serializable {
    private String examId;
    private String coordinatorId;
    private String reason;

    public ExamApprovalDecisionData() {
    }

    public ExamApprovalDecisionData(String examId, String coordinatorId, String reason) {
        this.examId = examId;
        this.coordinatorId = coordinatorId;
        this.reason = reason;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getCoordinatorId() {
        return coordinatorId;
    }

    public void setCoordinatorId(String coordinatorId) {
        this.coordinatorId = coordinatorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
