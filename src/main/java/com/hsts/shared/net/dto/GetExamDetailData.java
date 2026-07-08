package com.hsts.shared.net.dto;

import java.io.Serializable;

public class GetExamDetailData implements Serializable {
    private String examId;

    public GetExamDetailData() {
    }

    public GetExamDetailData(String examId) {
        this.examId = examId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }
}
