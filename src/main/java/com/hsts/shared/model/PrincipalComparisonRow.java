package com.hsts.shared.model;

import java.io.Serializable;
import java.util.Arrays;

/** One exam line in a Principal comparison report. */
public class PrincipalComparisonRow implements Serializable {

    private String examId;
    private String examTitle;
    private String courseId;
    private String teacherId;
    private int confirmedCount;
    private Double mean;
    private Double median;
    private int[] deciles = new int[10];
    private Double studentGrade;

    public PrincipalComparisonRow() {
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(int confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
    }

    public Double getMedian() {
        return median;
    }

    public void setMedian(Double median) {
        this.median = median;
    }

    public int[] getDeciles() {
        return deciles;
    }

    public void setDeciles(int[] deciles) {
        this.deciles = deciles != null ? deciles : new int[10];
    }

    public Double getStudentGrade() {
        return studentGrade;
    }

    public void setStudentGrade(Double studentGrade) {
        this.studentGrade = studentGrade;
    }

    @Override
    public String toString() {
        return "PrincipalComparisonRow{" +
                "examId='" + examId + '\'' +
                ", courseId='" + courseId + '\'' +
                ", confirmedCount=" + confirmedCount +
                ", mean=" + mean +
                ", median=" + median +
                ", studentGrade=" + studentGrade +
                ", deciles=" + Arrays.toString(deciles) +
                '}';
    }
}
