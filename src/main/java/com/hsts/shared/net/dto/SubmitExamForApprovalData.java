package com.hsts.shared.net.dto;

import java.io.Serializable;

public class SubmitExamForApprovalData implements Serializable {
    private String examId;
    private String teacherId;

    public SubmitExamForApprovalData() {
    }

    public SubmitExamForApprovalData(String examId, String teacherId) {
        this.examId = examId;
        this.teacherId = teacherId;
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
}
