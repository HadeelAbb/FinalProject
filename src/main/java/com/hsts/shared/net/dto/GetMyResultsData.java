package com.hsts.shared.net.dto;

import java.io.Serializable;

public class GetMyResultsData implements Serializable {
    private String studentId;

    public GetMyResultsData() {
    }

    public GetMyResultsData(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
