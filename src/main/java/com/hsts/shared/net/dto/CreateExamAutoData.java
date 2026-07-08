package com.hsts.shared.net.dto;

import com.hsts.shared.model.Difficulty;

import java.io.Serializable;

/**
 * SUC-3: teacher picks a topic/difficulty and a question count instead of
 * hand-picking questions; the server selects matching questions from the
 * bank. difficulty == null means "any difficulty".
 */
public class CreateExamAutoData implements Serializable {
    private String teacherId;
    private String courseId;
    private String title;
    private String instructionsForStudents;
    private String topic;
    private Difficulty difficulty;
    private int numberOfQuestions;
    private int durationMinutes;

    public CreateExamAutoData() {
    }

    public CreateExamAutoData(String teacherId, String courseId, String title, String instructionsForStudents,
                               String topic, Difficulty difficulty, int numberOfQuestions, int durationMinutes) {
        this.teacherId = teacherId;
        this.courseId = courseId;
        this.title = title;
        this.instructionsForStudents = instructionsForStudents;
        this.topic = topic;
        this.difficulty = difficulty;
        this.numberOfQuestions = numberOfQuestions;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(int numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
