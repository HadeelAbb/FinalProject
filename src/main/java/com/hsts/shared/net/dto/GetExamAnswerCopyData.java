package com.hsts.shared.net.dto;

import java.io.Serializable;

/** SUC-10: student requests a copy of her own graded exam (questions + her answers + correct answers). */
public class GetExamAnswerCopyData implements Serializable {
    private String examAnswerId;
    private String studentId;

    public GetExamAnswerCopyData() {
    }

    public GetExamAnswerCopyData(String examAnswerId, String studentId) {
        this.examAnswerId = examAnswerId;
        this.studentId = studentId;
    }

    public String getExamAnswerId() {
        return examAnswerId;
    }

    public void setExamAnswerId(String examAnswerId) {
        this.examAnswerId = examAnswerId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
