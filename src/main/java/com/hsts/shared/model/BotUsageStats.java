package com.hsts.shared.model;

import java.io.Serializable;

/**
 * Anonymized aggregate view for SUC-13 - a teacher sees usage counts per
 * course, never which student asked what (that stays private to the
 * student via SUC-15).
 */
public class BotUsageStats implements Serializable {

    private String courseId;
    private int totalQuestions;
    private int uniqueStudents;

    public BotUsageStats() {
    }

    public BotUsageStats(String courseId, int totalQuestions, int uniqueStudents) {
        this.courseId = courseId;
        this.totalQuestions = totalQuestions;
        this.uniqueStudents = uniqueStudents;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getUniqueStudents() {
        return uniqueStudents;
    }

    public void setUniqueStudents(int uniqueStudents) {
        this.uniqueStudents = uniqueStudents;
    }

    @Override
    public String toString() {
        return "Course " + courseId + ": " + totalQuestions + " questions from " + uniqueStudents + " students";
    }
}
