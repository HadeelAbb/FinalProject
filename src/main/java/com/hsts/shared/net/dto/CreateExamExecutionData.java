package com.hsts.shared.net.dto;

import java.io.Serializable;

/** SUC 2.2: a teacher opens a new execution (sitting) of an already-approved exam. */
public class CreateExamExecutionData implements Serializable {
    private String examId;
    private String teacherId;
    /** Format: dd-MM-yyyy HH:mm. */
    private String scheduledStart;
    private String scheduledEnd;
    /** Teacher-specified 4-character execution code (A-Z, 0-9). */
    private String executionCode;

    public CreateExamExecutionData() {
    }

    public CreateExamExecutionData(String examId, String teacherId, String scheduledStart, String scheduledEnd) {
        this(examId, teacherId, scheduledStart, scheduledEnd, null);
    }

    public CreateExamExecutionData(String examId, String teacherId, String scheduledStart, String scheduledEnd,
                                   String executionCode) {
        this.examId = examId;
        this.teacherId = teacherId;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.executionCode = executionCode;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(String scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public String getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(String scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public String getExecutionCode() {
        return executionCode;
    }

    public void setExecutionCode(String executionCode) {
        this.executionCode = executionCode;
    }
}