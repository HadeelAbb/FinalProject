package com.hsts.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SUC 2.2 / SUC-4: one "sitting" of an approved exam - each time a teacher
 * takes an exam "out of the drawer," a new execution is created with its
 * own 4-character code and its own open/close window. An exam can have
 * many executions over time, and each student submission belongs to
 * exactly one of them.
 */
public class ExamExecution implements Serializable {

    private String executionId;
    private String examId;
    private String executionCode;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    /** SUC-17: minutes added mid-execution - applies to this execution as a whole, not just currently-connected students. */
    private int extraMinutesGranted;
    private String createdByTeacherId;

    public ExamExecution() {
    }

    public ExamExecution(String executionId, String examId, String executionCode,
                         LocalDateTime scheduledStart, LocalDateTime scheduledEnd, String createdByTeacherId) {
        this.executionId = executionId;
        this.examId = examId;
        this.executionCode = executionCode;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.createdByTeacherId = createdByTeacherId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getExecutionCode() {
        return executionCode;
    }

    public void setExecutionCode(String executionCode) {
        this.executionCode = executionCode;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(LocalDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public LocalDateTime getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(LocalDateTime scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public int getExtraMinutesGranted() {
        return extraMinutesGranted;
    }

    public void setExtraMinutesGranted(int extraMinutesGranted) {
        this.extraMinutesGranted = extraMinutesGranted;
    }

    public String getCreatedByTeacherId() {
        return createdByTeacherId;
    }

    public void setCreatedByTeacherId(String createdByTeacherId) {
        this.createdByTeacherId = createdByTeacherId;
    }

    @Override
    public String toString() {
        return "Execution " + executionId + " (code " + executionCode + ") of exam " + examId
                + " by " + createdByTeacherId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExamExecution other)) return false;
        return executionId != null && executionId.equals(other.executionId);
    }

    @Override
    public int hashCode() {
        return executionId != null ? executionId.hashCode() : 0;
    }
}