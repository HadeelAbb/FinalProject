package server.controllers;

import com.hsts.shared.model.Question;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Focused checks for exam-question points validation and weighted grading.
 * No MySQL, OCSF, or Groq.
 */
public class ExamQuestionPointsValidatorTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        Map<String, Integer> valid = map("Q1", 20, "Q2", 30, "Q3", 50);
        check(ExamQuestionPointsValidator.validate(List.of("Q1", "Q2", "Q3"), valid) == null,
                "20 + 30 + 50 = 100 is accepted");

        Map<String, Integer> below = map("Q1", 20, "Q2", 30, "Q3", 40);
        checkEquals("Exam question points must total exactly 100. Current total: 90.",
                ExamQuestionPointsValidator.validate(List.of("Q1", "Q2", "Q3"), below),
                "20 + 30 + 40 = 90 is rejected");

        Map<String, Integer> above = map("Q1", 30, "Q2", 30, "Q3", 50);
        checkEquals("Exam question points must total exactly 100. Current total: 110.",
                ExamQuestionPointsValidator.validate(List.of("Q1", "Q2", "Q3"), above),
                "30 + 30 + 50 = 110 is rejected");

        Map<String, Integer> zero = map("Q1", 50, "Q2", 50, "Q3", 0);
        checkEquals("Each question's points must be greater than 0.",
                ExamQuestionPointsValidator.validate(List.of("Q1", "Q2", "Q3"), zero),
                "50 + 50 + 0 is rejected");

        Map<String, Integer> negative = map("Q1", 60, "Q2", 50, "Q3", -10);
        checkEquals("Each question's points must be greater than 0.",
                ExamQuestionPointsValidator.validate(List.of("Q1", "Q2", "Q3"), negative),
                "60 + 50 + (-10) is rejected even though the total is 100");

        check(ExamQuestionPointsValidator.validate(List.of("Q1", "Q2"), null) != null,
                "missing points map is rejected");

        Question q1 = question("Q1", 10);
        Question q2 = question("Q2", 20);
        Question q3 = question("Q3", 30);
        Question q4 = question("Q4", 40);
        Map<String, String> selected = Map.of("Q1", "yes", "Q2", "no", "Q3", "yes", "Q4", "no");
        Map<String, String> correct = Map.of("Q1", "yes", "Q2", "yes", "Q3", "yes", "Q4", "yes");
        check(ExamQuestionPointsValidator.grade(List.of(q1, q2, q3, q4), selected, correct) == 40,
                "weighted grade for Q1+Q3 correct is 40, not 50");

        Question examA = question("SHARED", 20);
        Question examB = question("SHARED", 10);
        Map<String, String> picked = Map.of("SHARED", "right");
        Map<String, String> right = Map.of("SHARED", "right");
        check(ExamQuestionPointsValidator.grade(List.of(examA), picked, right) == 20,
                "same question is worth 20 in exam A");
        check(ExamQuestionPointsValidator.grade(List.of(examB), picked, right) == 10,
                "same question is worth 10 in exam B");

        Map<String, Integer> forgedClientPoints = Map.of("Q1", 100, "Q2", 0, "Q3", 0, "Q4", 0);
        int official = ExamQuestionPointsValidator.grade(List.of(q1, q2, q3, q4), selected, correct);
        check(official == 40 && forgedClientPoints.get("Q1") == 100,
                "forged client point values are not used by grading");

        check(ExamQuestionPointsValidator.validateEqualSplit(4) == null,
                "auto split of 4 questions is allowed");
        check(ExamQuestionPointsValidator.validateEqualSplit(3) != null,
                "auto split of 3 questions is rejected");
        check(ExamQuestionPointsValidator.equalSplitPoints(5) == 20,
                "5 questions receive 20 points each");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static Question question(String id, int points) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setPoints(points);
        return q;
    }

    private static Map<String, Integer> map(String k1, int v1, String k2, int v2, String k3, int v3) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  OK  - " + description);
        } else {
            System.out.println("FAIL  - " + description);
            failCount++;
        }
    }

    private static void checkEquals(String expected, String actual, String description) {
        check(expected.equals(actual), description + " (got: " + actual + ")");
    }
}
