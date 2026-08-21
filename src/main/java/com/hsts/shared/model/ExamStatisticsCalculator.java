package com.hsts.shared.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mean, median, and decile distribution for a list of official grades.
 * Principal reports must pass confirmed scores only.
 */
public final class ExamStatisticsCalculator {

    private ExamStatisticsCalculator() {
    }

    public static final class Snapshot {
        public final int count;
        public final Double mean;
        public final Double median;
        public final int[] deciles;

        Snapshot(int count, Double mean, Double median, int[] deciles) {
            this.count = count;
            this.mean = mean;
            this.median = median;
            this.deciles = deciles != null ? deciles : new int[GradeHistogramCalculator.BUCKET_LABELS.length];
        }
    }

    /**
     * Official Principal score: confirmed final grade only.
     * Pending automatic grades return null.
     */
    public static Double officialConfirmedScore(ExamAnswer answer) {
        if (answer == null || !answer.isGradeConfirmed()) {
            return null;
        }
        if (answer.getFinalScore() != null) {
            return answer.getFinalScore();
        }
        return answer.getAutoScore();
    }

    public static Snapshot fromScores(List<Double> scores) {
        List<Double> sorted = new ArrayList<>();
        if (scores != null) {
            for (Double score : scores) {
                if (score != null) {
                    sorted.add(score);
                }
            }
        }
        Collections.sort(sorted);
        int[] deciles = GradeHistogramCalculator.countsFromGrades(sorted);
        if (sorted.isEmpty()) {
            return new Snapshot(0, null, null, deciles);
        }
        double sum = 0;
        for (double score : sorted) {
            sum += score;
        }
        double mean = round2(sum / sorted.size());
        double median;
        int total = sorted.size();
        if (total % 2 == 1) {
            median = sorted.get(total / 2);
        } else {
            median = (sorted.get((total / 2) - 1) + sorted.get(total / 2)) / 2.0;
        }
        return new Snapshot(total, mean, round2(median), deciles);
    }

    public static ExamStats toExamStats(String examId, List<Double> scores) {
        Snapshot snapshot = fromScores(scores);
        if (snapshot.count == 0) {
            return null;
        }
        return new ExamStats(examId, snapshot.count, snapshot.mean, snapshot.median, snapshot.deciles);
    }

    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
