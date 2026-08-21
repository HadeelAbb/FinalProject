package com.hsts.shared.model;

import java.util.List;

/**
 * Histogram bucket placement and display-grade fallback.
 */
public class GradeHistogramCalculatorTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        int[] normal = GradeHistogramCalculator.countsFromGrades(
                List.of(5.0, 12.0, 27.0, 44.0, 58.0, 63.0, 74.0, 85.0, 91.0, 100.0));
        checkCounts(normal, new int[]{1, 1, 1, 0, 1, 1, 1, 1, 1, 2},
                "normal distribution");

        List<Double> boundaries = List.of(0.0, 9.0, 10.0, 19.0, 20.0, 89.0, 90.0, 99.0, 100.0);
        int[] expectedIndex = {0, 0, 1, 1, 2, 8, 9, 9, 9};
        for (int i = 0; i < boundaries.size(); i++) {
            double grade = boundaries.get(i);
            check(GradeHistogramCalculator.bucketIndex(grade) == expectedIndex[i],
                    grade + " lands in bucket " + expectedIndex[i]);
        }
        int[] boundaryCounts = GradeHistogramCalculator.countsFromGrades(boundaries);
        check(sum(boundaryCounts) == boundaries.size(),
                "every boundary grade is in exactly one bucket");

        int[] empty = GradeHistogramCalculator.countsFromGrades(List.of());
        checkCounts(empty, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, "empty results");
        checkCounts(GradeHistogramCalculator.countsFromAnswers(List.of()),
                new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, "empty answers list");

        int[] one = GradeHistogramCalculator.countsFromGrades(List.of(75.0));
        checkCounts(one, new int[]{0, 0, 0, 0, 0, 0, 0, 1, 0, 0}, "single grade 75");

        ExamAnswer confirmed = answer(70.0, 85.0, true);
        check(confirmed.getDisplayScore() == 85.0, "confirmed result prefers final grade 85");
        checkCounts(GradeHistogramCalculator.countsFromAnswers(List.of(confirmed)),
                new int[]{0, 0, 0, 0, 0, 0, 0, 0, 1, 0}, "histogram uses confirmed 85");

        ExamAnswer pending = answer(70.0, 85.0, false);
        check(pending.getDisplayScore() == 70.0, "pending result uses automatic grade 70");
        checkCounts(GradeHistogramCalculator.countsFromAnswers(List.of(pending)),
                new int[]{0, 0, 0, 0, 0, 0, 0, 1, 0, 0}, "histogram uses pending auto 70");

        ExamAnswer weighted = answer(40.0, 40.0, false);
        checkCounts(GradeHistogramCalculator.countsFromAnswers(List.of(weighted)),
                new int[]{0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
                "weighted grade 40 is shown as 40, not equal-weight");

        check(GradeHistogramCalculator.bucketIndex(-1) == -1, "negative grade is not bucketed");
        check(GradeHistogramCalculator.bucketIndex(101) == -1, "grade above 100 is not bucketed");
        int[] invalid = GradeHistogramCalculator.countsFromGrades(List.of(-5.0, 101.0));
        checkCounts(invalid, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                "invalid grades do not create extra buckets");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static ExamAnswer answer(double auto, double fin, boolean confirmed) {
        ExamAnswer examAnswer = new ExamAnswer("EA1", "E1", "student1");
        examAnswer.setAutoScore(auto);
        examAnswer.setFinalScore(fin);
        examAnswer.setGradeConfirmed(confirmed);
        return examAnswer;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    private static void checkCounts(int[] actual, int[] expected, String description) {
        boolean same = actual != null && expected != null && actual.length == expected.length;
        if (same) {
            for (int i = 0; i < actual.length; i++) {
                if (actual[i] != expected[i]) {
                    same = false;
                    break;
                }
            }
        }
        check(same, description + " (got: " + java.util.Arrays.toString(actual) + ")");
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  OK  - " + description);
        } else {
            System.out.println("FAIL  - " + description);
            failCount++;
        }
    }
}
