package com.hsts.shared.net.dto;

import com.hsts.shared.model.Difficulty;

import java.io.Serializable;

/**
 * All fields are optional filters - null/blank means "don't filter on this field".
 */
public class SearchQuestionsData implements Serializable {
    private String courseId;
    private String topic;
    private Difficulty difficulty;

    public SearchQuestionsData() {
    }

    public SearchQuestionsData(String courseId, String topic, Difficulty difficulty) {
        this.courseId = courseId;
        this.topic = topic;
        this.difficulty = difficulty;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
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
}
