package com.hsts.shared.net.dto;

import java.io.Serializable;

/**
 * SUC-8: teacher confirms the auto-computed grade as-is, or overrides it -
 * either way requires a comment explaining the decision (per R-requirement
 * that a manual score change must be justified).
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
