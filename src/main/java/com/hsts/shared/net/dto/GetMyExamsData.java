package com.hsts.shared.net.dto;

import java.io.Serializable;

public class GetMyExamsData implements Serializable {
    private String teacherId;

    public GetMyExamsData() {
    }

    public GetMyExamsData(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
}
