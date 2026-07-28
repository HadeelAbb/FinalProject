package com.hsts.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side copy of Partner 1's Exam entity (PDOM).
 * Covers SUC-2 (manual build), SUC-3 (automatic build), SUC-4 (approval)
 * and the scheduling fields SUC-6/SUC-17 need at execution time.
 */
public class Exam implements Serializable {

    private String examId;
    private String courseId;
    private String title;
    private String instructionsForStudents;
    private List<Question> questions = new ArrayList<>();
    private int durationMinutes;
    private ExamStatus status = ExamStatus.DRAFT;
    private String createdByTeacherId;
    private String approvedByCoordinatorId;
    private String rejectionReason;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private String executionCode;

    public Exam() {
    }

    public Exam(String examId, String courseId, String title, String instructionsForStudents,
                List<Question> questions, int durationMinutes, String createdByTeacherId) {
        this.examId = examId;
        this.courseId = courseId;
        this.title = title;
        this.instructionsForStudents = instructionsForStudents;
        this.questions = questions != null ? questions : new ArrayList<>();
        this.durationMinutes = durationMinutes;
        this.createdByTeacherId = createdByTeacherId;
    }
    // Overloaded constructor supporting executionCode
    public Exam(String examId, String courseId, String title, String instructionsForStudents,
                List<Question> questions, int durationMinutes, String createdByTeacherId, String executionCode) {
        this(examId, courseId, title, instructionsForStudents, questions, durationMinutes, createdByTeacherId);
        this.executionCode = executionCode;
    }

    public int totalPoints() {
        return questions.isEmpty() ? 0 : 100;
    }

    public double pointsPerQuestion() {
        return questions.isEmpty() ? 0 : 100.0 / questions.size();
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructionsForStudents() {
        return instructionsForStudents;
    }

    public void setInstructionsForStudents(String instructionsForStudents) {
        this.instructionsForStudents = instructionsForStudents;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = status;
    }

    public String getCreatedByTeacherId() {
        return createdByTeacherId;
    }

    public void setCreatedByTeacherId(String createdByTeacherId) {
        this.createdByTeacherId = createdByTeacherId;
    }

    public String getApprovedByCoordinatorId() {
        return approvedByCoordinatorId;
    }

    public void setApprovedByCoordinatorId(String approvedByCoordinatorId) {
        this.approvedByCoordinatorId = approvedByCoordinatorId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(LocalDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public LocalDateTime getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(LocalDateTime scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    @Override
    public String toString() {
        return "[" + examId + "] " + title + " (" + status + ")";
    }


    public String getExecutionCode() {
        return executionCode;
    }

    public void setExecutionCode(String executionCode) {
        this.executionCode = executionCode;
    }

}
