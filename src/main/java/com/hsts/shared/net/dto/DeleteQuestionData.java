package com.hsts.shared.net.dto;

import java.io.Serializable;

public class DeleteQuestionData implements Serializable {
    private String questionId;
    private String teacherId;

    public DeleteQuestionData() {
    }

    public DeleteQuestionData(String questionId, String teacherId) {
        this.questionId = questionId;
        this.teacherId = teacherId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
}
