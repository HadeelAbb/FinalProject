package com.hsts.shared.net.dto;

import java.io.Serializable;

/**
 * SUC-8: teacher confirms the auto-computed grade as-is, or overrides it.
 * A non-blank teacherComment is required only when finalScore differs from
 * the official stored autoScore. The client must not send autoScore; the
 * server compares against ExamAnswer.autoScore from storage.
 */
public class ConfirmGradeData implements Serializable {
    private String examAnswerId;
    private String teacherId;
    private Double finalScore;
    private String teacherComment;

    public ConfirmGradeData() {
    }

    public ConfirmGradeData(String examAnswerId, String teacherId, Double finalScore, String teacherComment) {
        this.examAnswerId = examAnswerId;
        this.teacherId = teacherId;
        this.finalScore = finalScore;
        this.teacherComment = teacherComment;
    }

    public String getExamAnswerId() {
        return examAnswerId;
    }

    public void setExamAnswerId(String examAnswerId) {
        this.examAnswerId = examAnswerId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
    }

    public String getTeacherComment() {
        return teacherComment;
    }

    public void setTeacherComment(String teacherComment) {
        this.teacherComment = teacherComment;
    }
}
