package com.hsts.shared.net.dto;

import java.io.Serializable;

public class UpdateBotConfigData implements Serializable {
    private String courseId;
    private String teacherId;
    private String botName;
    private String knowledgeSources;
    private boolean active;

    public UpdateBotConfigData() {
    }

    public UpdateBotConfigData(String courseId, String teacherId, String botName,
                               String knowledgeSources, boolean active) {
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.botName = botName;
        this.knowledgeSources = knowledgeSources;
        this.active = active;
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

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getKnowledgeSources() {
        return knowledgeSources;
    }

    public void setKnowledgeSources(String knowledgeSources) {
        this.knowledgeSources = knowledgeSources;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}