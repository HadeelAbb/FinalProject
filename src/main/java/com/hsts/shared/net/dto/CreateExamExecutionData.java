package com.hsts.shared.net.dto;

import java.io.Serializable;

/** SUC 2.2: a teacher opens a new execution (sitting) of an already-approved exam. */
public class CreateExamExecutionData implements Serializable {
    private String examId;
    private String teacherId;
    /** Format: yyyy-MM-dd HH:mm. Optional - blank uses the default window. */
    private String scheduledStart;
    private String scheduledEnd;

    public CreateExamExecutionData() {
    }

    public CreateExamExecutionData(String examId, String teacherId, String scheduledStart, String scheduledEnd) {
        this.examId = examId;
        this.teacherId = teacherId;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
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
}