package com.hsts.shared.net.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Creates a new immutable Exam version from an existing current version.
 * Course is taken from the source exam, not from this DTO.
 * teacherId is overwritten by RequestIdentityBinder.
 */
public class CreateExamVersionData implements Serializable {
    private String sourceExamId;
    private String teacherId;
    private String title;
    private String instructionsForStudents;
    private String instructionsForTeacher;
    private List<String> questionIds;
    private Map<String, Integer> questionPoints;
    private int durationMinutes;

    public CreateExamVersionData() {
    }

    public CreateExamVersionData(String sourceExamId, String teacherId, String title,
                                 String instructionsForStudents, String instructionsForTeacher,
                                 List<String> questionIds, Map<String, Integer> questionPoints,
                                 int durationMinutes) {
        this.sourceExamId = sourceExamId;
        this.teacherId = teacherId;
        this.title = title;
        this.instructionsForStudents = instructionsForStudents;
        this.instructionsForTeacher = instructionsForTeacher;
        this.questionIds = questionIds;
        this.questionPoints = questionPoints;
        this.durationMinutes = durationMinutes;
    }

    public String getSourceExamId() {
        return sourceExamId;
    }

    public void setSourceExamId(String sourceExamId) {
        this.sourceExamId = sourceExamId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
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

    public String getInstructionsForTeacher() {
        return instructionsForTeacher;
    }

    public void setInstructionsForTeacher(String instructionsForTeacher) {
        this.instructionsForTeacher = instructionsForTeacher;
    }

    public List<String> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<String> questionIds) {
        this.questionIds = questionIds;
    }

    public Map<String, Integer> getQuestionPoints() {
        return questionPoints;
    }

    public void setQuestionPoints(Map<String, Integer> questionPoints) {
        this.questionPoints = questionPoints;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
