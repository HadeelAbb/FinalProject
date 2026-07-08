package com.hsts.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Client-side copy of Partner 1's BotInteraction entity (PDOM).
 * One record per question a student asks the study bot (SUC-14). Feeds
 * both the student's own history (SUC-15) and the teacher's anonymized
 * usage stats (SUC-13 - aggregate only, never shown per-student to the
 * teacher, per the requirement that usage stats don't identify users).
 */
public class BotInteraction implements Serializable {

    private String interactionId;
    private String studentId;
    private String courseId;
    private String question;
    private String answer;
    private LocalDateTime askedAt;

    public BotInteraction() {
    }

    public BotInteraction(String interactionId, String studentId, String courseId, String question, String answer) {
        this.interactionId = interactionId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.question = question;
        this.answer = answer;
        this.askedAt = LocalDateTime.now();
    }

    public String getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(String interactionId) {
        this.interactionId = interactionId;
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

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public LocalDateTime getAskedAt() {
        return askedAt;
    }

    public void setAskedAt(LocalDateTime askedAt) {
        this.askedAt = askedAt;
    }
}
