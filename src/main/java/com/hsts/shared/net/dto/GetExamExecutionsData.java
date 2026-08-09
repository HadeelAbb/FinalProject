package com.hsts.shared.net.dto;

import java.io.Serializable;

/** Lists every execution (past and present) that has ever been run for one exam. */
public class GetExamExecutionsData implements Serializable {
    private String examId;

    public GetExamExecutionsData() {
    }

    public GetExamExecutionsData(String examId) {
        this.examId = examId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }
}