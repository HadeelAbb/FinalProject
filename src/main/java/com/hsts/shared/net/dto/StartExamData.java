package com.hsts.shared.net.dto;

import java.io.Serializable;

public class StartExamData implements Serializable {
    private String examId;
    private String studentId;

    public StartExamData() {
    }

    public StartExamData(String examId, String studentId) {
        this.examId = examId;
        this.studentId = studentId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
