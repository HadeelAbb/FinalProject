package com.hsts.shared.net.dto;

import java.io.Serializable;

/** SUC-17: teacher extends the time for an in-progress exam (temporary, this run only). */
public class ExtendExamTimeData implements Serializable {
    private String examId;
    private String teacherId;
    private int additionalMinutes;

    public ExtendExamTimeData() {
    }

    public ExtendExamTimeData(String examId, String teacherId, int additionalMinutes) {
        this.examId = examId;
        this.teacherId = teacherId;
        this.additionalMinutes = additionalMinutes;
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

    public int getAdditionalMinutes() {
        return additionalMinutes;
    }

    public void setAdditionalMinutes(int additionalMinutes) {
        this.additionalMinutes = additionalMinutes;
    }
}
