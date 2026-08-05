package com.hsts.shared.net.dto;

import java.io.Serializable;

/** SUC 5 / 7.2 / 7.3.2: request statistical metrics (mean/median/deciles) for one exam. */
public class GetExamStatsData implements Serializable {
    private String examId;

    public GetExamStatsData() {
    }

    public GetExamStatsData(String examId) {
        this.examId = examId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }
}