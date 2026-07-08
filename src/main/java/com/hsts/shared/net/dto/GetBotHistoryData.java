package com.hsts.shared.net.dto;

import java.io.Serializable;

public class GetBotHistoryData implements Serializable {
    private String studentId;

    public GetBotHistoryData() {
    }

    public GetBotHistoryData(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
