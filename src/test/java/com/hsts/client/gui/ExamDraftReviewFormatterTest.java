package com.hsts.client.gui;

import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamStatus;
import com.hsts.shared.model.Question;

import java.util.List;

/**
 * Confirms the draft review shows assigned exam-question points, not 100 / n.
 */
public class ExamDraftReviewFormatterTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        Question q1 = question("Q1", "What is ...?", 10);
        Question q2 = question("Q2", "Which of the following ...?", 20);
        Question q3 = question("Q3", "Explain ...", 30);
        Question q4 = question("Q4", "Choose ...", 40);
        Exam weighted = exam("Weighted draft", List.of(q1, q2, q3, q4));

        check(ExamDraftReviewFormatter.formatQuestion(q1).contains("Q1"), "question id is shown");
        check(ExamDraftReviewFormatter.formatQuestion(q1).contains("What is ...?"), "question text is shown");
        check(ExamDraftReviewFormatter.formatQuestion(q1).contains("10 points"), "Q1 shows 10 points");
        check(ExamDraftReviewFormatter.formatQuestion(q2).contains("20 points"), "Q2 shows 20 points");
        check(ExamDraftReviewFormatter.formatQuestion(q3).contains("30 points"), "Q3 shows 30 points");
        check(ExamDraftReviewFormatter.formatQuestion(q4).contains("40 points"), "Q4 shows 40 points");
        check(!ExamDraftReviewFormatter.formatQuestion(q1).contains("25 points"),
                "review does not show equal-weight 25 for a 10-point question");
        check(ExamDraftReviewFormatter.sumAssignedPoints(weighted) == 100,
                "weighted exam sums stored points to 100");
        checkEquals("Total: 100 / 100", ExamDraftReviewFormatter.formatTotal(weighted),
                "weighted total uses stored points");

        Exam autoFive = exam("Auto five", List.of(
                question("11001", "A", 20),
                question("11002", "B", 20),
                question("11003", "C", 20),
                question("11004", "D", 20),
                question("11005", "E", 20)));
        check(ExamDraftReviewFormatter.formatQuestions(autoFive).size() == 5,
                "automatic 5-question draft lists 5 questions");
        check(ExamDraftReviewFormatter.sumAssignedPoints(autoFive) == 100,
                "automatic 5 x 20 totals 100");
        checkEquals("Total: 100 / 100", ExamDraftReviewFormatter.formatTotal(autoFive),
                "automatic 5-question total is 100 / 100");
        check(ExamDraftReviewFormatter.formatMeta(autoFive).contains("DRAFT"),
                "status is shown");
        check(ExamDraftReviewFormatter.formatMeta(autoFive).contains("CS101"),
                "course is shown");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static Exam exam(String title, List<Question> questions) {
        Exam exam = new Exam("E1", "CS101", title, "instructions", questions, 10, "teacher1");
        exam.setStatus(ExamStatus.DRAFT);
        return exam;
    }

    private static Question question(String id, String text, int points) {
        Question question = new Question();
        question.setQuestionId(id);
        question.setText(text);
        question.setTopic("Architecture");
        question.setDifficulty(Difficulty.EASY);
        question.setPoints(points);
        return question;
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
