package com.hsts.shared.net.dto;

import java.io.Serializable;

public class GetAvailableExamsData implements Serializable {
    private String studentId;

    public GetAvailableExamsData() {
    }

    public GetAvailableExamsData(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
