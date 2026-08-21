package com.hsts.shared.net.dto;

import java.io.Serializable;
import java.util.List;

public class CreateExamManualData implements Serializable {
    private String teacherId;
    private String courseId;
    private String title;
    private String instructionsForStudents;
    private String instructionsForTeacher;
    private List<String> questionIds;
    /** questionId -> positive integer points for this exam only. */
    private java.util.Map<String, Integer> questionPoints;
    private int durationMinutes;

    public CreateExamManualData() {
    }

    // Backward-compatible for older callers (e.g. ExamBuildTestDriver) that don't supply teacher notes.
    public CreateExamManualData(String teacherId, String courseId, String title, String instructionsForStudents,
                                List<String> questionIds, int durationMinutes) {
        this(teacherId, courseId, title, instructionsForStudents, null, questionIds, durationMinutes);
    }

    public CreateExamManualData(String teacherId, String courseId, String title, String instructionsForStudents,
                                String instructionsForTeacher, List<String> questionIds, int durationMinutes) {
        this.teacherId = teacherId;
        this.courseId = courseId;
        this.title = title;
        this.instructionsForStudents = instructionsForStudents;
        this.instructionsForTeacher = instructionsForTeacher;
        this.questionIds = questionIds;
        this.durationMinutes = durationMinutes;
    }

    public CreateExamManualData(String teacherId, String courseId, String title, String instructionsForStudents,
                                String instructionsForTeacher, List<String> questionIds,
                                java.util.Map<String, Integer> questionPoints, int durationMinutes) {
        this(teacherId, courseId, title, instructionsForStudents, instructionsForTeacher, questionIds, durationMinutes);
        this.questionPoints = questionPoints;
    }

    public String getInstructionsForTeacher() {
        return instructionsForTeacher;
    }

    public void setInstructionsForTeacher(String instructionsForTeacher) {
        this.instructionsForTeacher = instructionsForTeacher;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
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

    public List<String> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<String> questionIds) {
        this.questionIds = questionIds;
    }

    public java.util.Map<String, Integer> getQuestionPoints() {
        return questionPoints;
    }

    public void setQuestionPoints(java.util.Map<String, Integer> questionPoints) {
        this.questionPoints = questionPoints;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}