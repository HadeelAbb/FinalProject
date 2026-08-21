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
    private String instructionsForTeacher;
    private List<Question> questions = new ArrayList<>();
    private int durationMinutes;
    /**
     * Populated only on START_EXAM: remaining personal exam seconds for this
     * student's sitting, derived from original startedAt. Not the base duration
     * and not ExamExecution.scheduled_start.
     */
    private Integer remainingSeconds;
    private ExamStatus status = ExamStatus.DRAFT;
    private String createdByTeacherId;
    private String approvedByCoordinatorId;
    private String rejectionReason;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private String executionCode;
    /**
     * Physical exam_id of version 1 in this lineage. Each version is its own
     * exams row; this only groups versions of the same logical exam.
     */
    private String rootExamId;
    private int versionNumber = 1;
    private boolean latest = true;

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
        this.rootExamId = examId;
        this.versionNumber = 1;
        this.latest = true;
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

    /** SUC-3.2: internal notes only the teacher sees - never shown to students taking the exam. */
    public String getInstructionsForTeacher() {
        return instructionsForTeacher;
    }

    public void setInstructionsForTeacher(String instructionsForTeacher) {
        this.instructionsForTeacher = instructionsForTeacher;
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

    public Integer getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Integer remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
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

    public String getExecutionCode() {
        return executionCode;
    }

    public void setExecutionCode(String executionCode) {
        this.executionCode = executionCode;
    }

    public String getRootExamId() {
        return rootExamId != null && !rootExamId.isBlank() ? rootExamId : examId;
    }

    public void setRootExamId(String rootExamId) {
        this.rootExamId = rootExamId;
    }

    public int getVersionNumber() {
        return versionNumber <= 0 ? 1 : versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }

    public String versionStatusLabel() {
        return latest ? "Current" : "Historical";
    }

    @Override
    public String toString() {
        return "[" + examId + "] v" + getVersionNumber() + " " + versionStatusLabel()
                + " " + title + " (" + status + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exam other)) return false;
        return examId != null && examId.equals(other.examId);
    }

    @Override
    public int hashCode() {
        return examId != null ? examId.hashCode() : 0;
    }
}