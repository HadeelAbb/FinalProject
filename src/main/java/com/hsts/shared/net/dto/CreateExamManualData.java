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
    private int durationMinutes;

    public CreateExamManualData() {
    }

    public CreateExamManualData(String teacherId, String courseId, String title, String instructionsForStudents,
                                List<String> questionIds, int durationMinutes) {
        this.teacherId = teacherId;
        this.courseId = courseId;
        this.title = title;
        this.instructionsForStudents = instructionsForStudents;
        this.questionIds = questionIds;
        this.durationMinutes = durationMinutes;
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}