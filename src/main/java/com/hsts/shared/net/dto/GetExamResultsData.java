package com.hsts.shared.net.dto;

import java.io.Serializable;

/** Teacher request for all submitted results of one exam they own. */
public class GetExamResultsData implements Serializable {
    private String examId;
    private String teacherId;

    public GetExamResultsData() {
    }

    public GetExamResultsData(String examId, String teacherId) {
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
