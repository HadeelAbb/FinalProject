package com.hsts.shared.net.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SubmitExamData implements Serializable {
    private String examId;
    private String studentId;
    /** questionId -> selected answer text. */
    private Map<String, String> selectedAnswers = new HashMap<>();
    private boolean autoSubmitted;

    public SubmitExamData() {
    }

    public SubmitExamData(String examId, String studentId, Map<String, String> selectedAnswers,
                           boolean autoSubmitted) {
        this.examId = examId;
        this.studentId = studentId;
        this.selectedAnswers = selectedAnswers != null ? selectedAnswers : new HashMap<>();
        this.autoSubmitted = autoSubmitted;
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

    public Map<String, String> getSelectedAnswers() {
        return selectedAnswers;
    }

    public void setSelectedAnswers(Map<String, String> selectedAnswers) {
        this.selectedAnswers = selectedAnswers;
    }

    public boolean isAutoSubmitted() {
        return autoSubmitted;
    }

    public void setAutoSubmitted(boolean autoSubmitted) {
        this.autoSubmitted = autoSubmitted;
    }
}
