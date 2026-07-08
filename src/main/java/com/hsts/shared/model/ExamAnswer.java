package com.hsts.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side copy of Partner 1's ExamAnswer entity (PDOM).
 * One instance per student per exam - covers SUC-6 (taking, via answers map
 * filled in progressively), SUC-7 (autoScore, filled in by the server the
 * moment the student submits), SUC-8 (finalScore/teacherComment/graded,
 * filled in when the teacher confirms or overrides the grade), and SUC-10
 * (the whole object is what the student sees when viewing her results).
 */
public class ExamAnswer implements Serializable {

    private String examAnswerId;
    private String examId;
    private String studentId;

    /** questionId -> the answer text the student selected. */
    private Map<String, String> selectedAnswers = new HashMap<>();

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private boolean autoSubmitted;

    private Double autoScore;
    private Double finalScore;
    private String teacherComment;
    private boolean gradeConfirmed;

    /** Extra minutes granted mid-exam by a teacher (SUC-17). */
    private int extraMinutesGranted;

    public ExamAnswer() {
    }

    public ExamAnswer(String examAnswerId, String examId, String studentId) {
        this.examAnswerId = examAnswerId;
        this.examId = examId;
        this.studentId = studentId;
    }

    public String getExamAnswerId() {
        return examAnswerId;
    }

    public void setExamAnswerId(String examAnswerId) {
        this.examAnswerId = examAnswerId;
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
        this.selectedAnswers = selectedAnswers != null ? selectedAnswers : new HashMap<>();
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isAutoSubmitted() {
        return autoSubmitted;
    }

    public void setAutoSubmitted(boolean autoSubmitted) {
        this.autoSubmitted = autoSubmitted;
    }

    public Double getAutoScore() {
        return autoScore;
    }

    public void setAutoScore(Double autoScore) {
        this.autoScore = autoScore;
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

    public boolean isGradeConfirmed() {
        return gradeConfirmed;
    }

    public void setGradeConfirmed(boolean gradeConfirmed) {
        this.gradeConfirmed = gradeConfirmed;
    }

    public int getExtraMinutesGranted() {
        return extraMinutesGranted;
    }

    public void setExtraMinutesGranted(int extraMinutesGranted) {
        this.extraMinutesGranted = extraMinutesGranted;
    }

    /** The score to actually display/report: teacher's override if confirmed, else the auto score. */
    public Double getDisplayScore() {
        return gradeConfirmed && finalScore != null ? finalScore : autoScore;
    }
}
