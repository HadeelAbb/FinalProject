package com.hsts.shared.net.dto;

import java.io.Serializable;

public class AskBotQuestionData implements Serializable {
    private String studentId;
    private String courseId;
    private String question;

    public AskBotQuestionData() {
    }

    public AskBotQuestionData(String studentId, String courseId, String question) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.question = question;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
