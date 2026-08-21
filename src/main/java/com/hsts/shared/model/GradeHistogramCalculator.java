package com.hsts.shared.model;

import java.util.List;

/**
 * Ten non-overlapping grade buckets for teacher results.
 * 90–100 includes 100. Values outside 0–100 are not placed in any bucket.
 */
public final class GradeHistogramCalculator {

    public static final String[] BUCKET_LABELS = {
            "0-9", "10-19", "20-29", "30-39", "40-49",
            "50-59", "60-69", "70-79", "80-89", "90-100"
    };

    private GradeHistogramCalculator() {
    }

    /**
     * @return 0–9 for a valid grade, or -1 if the grade is outside 0–100.
     */
    public static int bucketIndex(double grade) {
        if (grade < 0 || grade > 100) {
            return -1;
        }
        if (grade >= 90) {
            return 9;
        }
        return (int) (grade / 10.0);
    }

    public static int[] countsFromGrades(Iterable<Double> grades) {
        int[] counts = new int[BUCKET_LABELS.length];
        if (grades == null) {
            return counts;
        }
        for (Double grade : grades) {
            if (grade == null) {
                continue;
            }
            int index = bucketIndex(grade);
            if (index >= 0) {
                counts[index]++;
            }
        }
        return counts;
    }

    /**
     * Uses {@link ExamAnswer#getDisplayScore()}: confirmed final grade when present,
     * otherwise the automatic grade.
     */
    public static int[] countsFromAnswers(List<ExamAnswer> answers) {
        int[] counts = new int[BUCKET_LABELS.length];
        if (answers == null) {
            return counts;
        }
        for (ExamAnswer answer : answers) {
            if (answer == null || answer.getDisplayScore() == null) {
                continue;
            }
            int index = bucketIndex(answer.getDisplayScore());
            if (index >= 0) {
                counts[index]++;
            }
        }
        return counts;
    }
}
