package com.hsts.shared.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * DTO holding statistical metric calculations (Section 5 / 7.2 / 7.3).
 */
public class ExamStats implements Serializable {

    private String examId;
    private int totalSubmissions;
    private double mean;
    private double median;
    /** Array of size 10 holding counts for score ranges: 0-9, 10-19, ..., 90-100 */
    private int[] deciles = new int[10];

    public ExamStats() {
    }

    public ExamStats(String examId, int totalSubmissions, double mean, double median, int[] deciles) {
        this.examId = examId;
        this.totalSubmissions = totalSubmissions;
        this.mean = mean;
        this.median = median;
        this.deciles = deciles != null ? deciles : new int[10];
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public int getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(int totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getMedian() {
        return median;
    }

    public void setMedian(double median) {
        this.median = median;
    }

    public int[] getDeciles() {
        return deciles;
    }

    public void setDeciles(int[] deciles) {
        this.deciles = deciles;
    }

    @Override
    public String toString() {
        return "ExamStats{" +
                "examId='" + examId + '\'' +
                ", totalSubmissions=" + totalSubmissions +
                ", mean=" + mean +
                ", median=" + median +
                ", deciles=" + Arrays.toString(deciles) +
                '}';
    }
}