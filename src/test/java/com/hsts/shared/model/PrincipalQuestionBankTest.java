package com.hsts.shared.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Principal read-only Question Bank filters and detail formatting.
 * No MySQL, OCSF, or JavaFX.
 */
public class PrincipalQuestionBankTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        Question cs = question("11001", "CS101", "Architecture", Difficulty.EASY,
                "What is the main function of the CPU?",
                List.of("Execute instructions", "Store files", "Display graphics", "Route packets"), 0);
        Question math = question("22001", "MATH201", "Calculus", Difficulty.HARD,
                "What is the derivative of x^2?",
                List.of("2x", "x", "x^2", "2"), 0);

        List<Question> all = List.of(cs, math);
        List<Question> allCourses = filter(all, null, null, null);
        check(allCourses.size() == 2, "Principal all-question view contains CS101 and MATH201");
        check(containsCourse(allCourses, "CS101") && containsCourse(allCourses, "MATH201"),
                "multiple courses are returned together");

        List<Question> csOnly = filter(all, "CS101", null, null);
        check(csOnly.size() == 1 && "CS101".equals(csOnly.get(0).getCourseId()),
                "Course = CS101 returns CS101 questions only");
        check(!containsCourse(csOnly, "MATH201"), "MATH201 is excluded from CS101 filter");

        List<Question> empty = filter(List.of(), null, null, null);
        check(empty.isEmpty(), "empty bank returns an empty list");
        check(PrincipalQuestionDetailFormatter.emptyBankMessage().equals("No questions available."),
                "empty bank has a clear empty-state message");
        check(PrincipalQuestionDetailFormatter.format(null).contains("No question selected"),
                "formatter does not crash on a missing selection");

        String detail = PrincipalQuestionDetailFormatter.format(cs);
        check(detail.contains("11001"), "detail includes question ID");
        check(detail.contains("Version: 1"), "detail includes version number");
        check(detail.contains("Status: Current"), "detail includes current status");
        check(detail.contains("CS101"), "detail includes course");
        check(detail.contains("Architecture"), "detail includes topic");
        check(detail.contains("EASY"), "detail includes difficulty");
        check(detail.contains("What is the main function of the CPU?"), "detail includes question text");
        check(detail.contains("A. Execute instructions"), "detail includes answer A");
        check(detail.contains("B. Store files"), "detail includes answer B");
        check(detail.contains("C. Display graphics"), "detail includes answer C");
        check(detail.contains("D. Route packets"), "detail includes answer D");
        check(detail.contains("Correct answer: A"), "Principal can see the correct answer");

        cs.setImageData(com.hsts.shared.model.QuestionIllustrationTest.pngA());
        cs.setImagePath("cpu.png");
        String withImage = PrincipalQuestionDetailFormatter.format(cs);
        check(withImage.contains("Illustration: cpu.png"), "detail mentions attached illustration filename");
        check(!withImage.contains("C:\\"), "detail does not expose a teacher-local path");

        check(PrincipalQuestionFilter.matches(cs, null, "arch", null),
                "topic contains-filter is case-insensitive");
        check(!PrincipalQuestionFilter.matches(cs, null, null, Difficulty.HARD),
                "difficulty filter excludes non-matching questions");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static List<Question> filter(List<Question> questions, String courseId, String topic,
                                         Difficulty difficulty) {
        List<Question> matches = new ArrayList<>();
        for (Question question : questions) {
            if (PrincipalQuestionFilter.matches(question, courseId, topic, difficulty)) {
                matches.add(question);
            }
        }
        return matches;
    }

    private static boolean containsCourse(List<Question> questions, String courseId) {
        for (Question question : questions) {
            if (courseId.equals(question.getCourseId())) {
                return true;
            }
        }
        return false;
    }

    private static Question question(String id, String courseId, String topic, Difficulty difficulty,
                                     String text, List<String> answers, int correctIndex) {
        List<QuestionAnswer> options = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            options.add(new QuestionAnswer(answers.get(i), i == correctIndex));
        }
        return new Question(id, text, "Select one.", difficulty, topic, null, courseId, options);
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
