package com.hsts.shared.net.dto;

import java.io.Serializable;

public class GetPendingGradingData implements Serializable {
    private String teacherId;

    public GetPendingGradingData() {
    }

    public GetPendingGradingData(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
}
