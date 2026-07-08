package com.hsts.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side copy of Partner 1's Question entity (PDOM).
 * questionId format matches the SQL schema: 3-digit sequence + 2-digit
 * course code (e.g. "00111" = question 001 in course 11).
 */
public class Question implements Serializable {

    private String questionId;
    private String text;
    private String instructions;
    private Difficulty difficulty;
    private String topic;
    private String imagePath;
    private String courseId;
    private List<QuestionAnswer> answers = new ArrayList<>();

    public Question() {
    }

    public Question(String questionId, String text, String instructions, Difficulty difficulty,
                    String topic, String imagePath, String courseId, List<QuestionAnswer> answers) {
        this.questionId = questionId;
        this.text = text;
        this.instructions = instructions;
        this.difficulty = difficulty;
        this.topic = topic;
        this.imagePath = imagePath;
        this.courseId = courseId;
        this.answers = answers != null ? answers : new ArrayList<>();
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public List<QuestionAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuestionAnswer> answers) {
        this.answers = answers;
    }

    public QuestionAnswer getCorrectAnswer() {
        return answers.stream().filter(QuestionAnswer::isCorrect).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return "[" + questionId + "] (" + topic + " / " + difficulty + ") " + text;
    }
}
