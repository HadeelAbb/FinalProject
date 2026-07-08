package com.hsts.shared.net.dto;

import java.io.Serializable;

public class GetBotUsageStatsData implements Serializable {
    private String teacherId;

    public GetBotUsageStatsData() {
    }

    public GetBotUsageStatsData(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
}
