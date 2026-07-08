package com.hsts.shared.net.dto;

import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.QuestionAnswer;

import java.io.Serializable;
import java.util.List;

public class CreateQuestionData implements Serializable {
    private String text;
    private String instructions;
    private Difficulty difficulty;
    private String topic;
    private String imagePath;
    private String courseId;
    private String teacherId;
    private List<QuestionAnswer> answers;

    public CreateQuestionData() {
    }

    public CreateQuestionData(String text, String instructions, Difficulty difficulty, String topic,
                              String imagePath, String courseId, String teacherId,
                              List<QuestionAnswer> answers) {
        this.text = text;
        this.instructions = instructions;
        this.difficulty = difficulty;
        this.topic = topic;
        this.imagePath = imagePath;
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.answers = answers;
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

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public List<QuestionAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuestionAnswer> answers) {
        this.answers = answers;
    }
}
