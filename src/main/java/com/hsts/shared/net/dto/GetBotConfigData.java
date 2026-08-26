package com.hsts.shared.net.dto;

import java.io.Serializable;

public class GetBotConfigData implements Serializable {
    private String courseId;

    public GetBotConfigData() {
    }

    public GetBotConfigData(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
}