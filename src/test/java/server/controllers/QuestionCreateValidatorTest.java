package server.controllers;

import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;

import java.util.ArrayList;
import java.util.List;

/**
 * Focused, non-destructive checks for CREATE QUESTION server-side rules.
 * Does not connect to MySQL or send OCSF traffic.
 */
public class QuestionCreateValidatorTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        check(QuestionCreateValidator.validate(validPayload()) == null,
                "valid 4 answers / 1 correct is accepted");
        check(QuestionCreateValidator.validate(validPayload()) == null,
                "create without image is accepted");

        CreateQuestionData withPng = validPayload();
        withPng.setImageData(com.hsts.shared.model.QuestionIllustrationTest.pngA());
        withPng.setImagePath("C:\\Users\\Teacher\\Desktop\\a.png");
        check(QuestionCreateValidator.validate(withPng) == null, "create with PNG is accepted");

        CreateQuestionData badImage = validPayload();
        badImage.setImageData(new byte[]{1, 2, 3, 4});
        checkEquals(com.hsts.shared.model.QuestionIllustration.NOT_IMAGE,
                QuestionCreateValidator.validate(badImage),
                "non-image bytes are rejected");

        checkEquals("A question must contain exactly 4 answers.",
                QuestionCreateValidator.validate(payloadWithAnswerCount(2)),
                "2 answers are rejected");
        checkEquals("A question must contain exactly 4 answers.",
                QuestionCreateValidator.validate(payloadWithAnswerCount(3)),
                "3 answers are rejected");
        checkEquals("A question must contain exactly 4 answers.",
                QuestionCreateValidator.validate(payloadWithAnswerCount(5)),
                "5 answers are rejected");
        checkEquals("A question must contain exactly 4 answers.",
                QuestionCreateValidator.validateAnswers(null),
                "null answer list is rejected");

        checkEquals("A question must contain exactly one correct answer.",
                QuestionCreateValidator.validate(payloadWithCorrectCount(0)),
                "zero correct answers are rejected");
        checkEquals("A question must contain exactly one correct answer.",
                QuestionCreateValidator.validate(payloadWithCorrectCount(2)),
                "multiple correct answers are rejected");

        checkEquals("All four answers must be non-empty.",
                QuestionCreateValidator.validate(payloadWithBlankAnswer()),
                "blank answers are rejected");

        CreateQuestionData emptyText = validPayload();
        emptyText.setText("   ");
        checkEquals("Question text is required.",
                QuestionCreateValidator.validate(emptyText),
                "blank question text is rejected");

        EditQuestionData validEdit = new EditQuestionData("Q1", "Updated", "", Difficulty.EASY, "topic",
                null, "teacher1", validPayload().getAnswers());
        check(QuestionCreateValidator.validate(validEdit) == null, "valid edit answers are accepted");
        EditQuestionData shortEdit = new EditQuestionData("Q1", "Updated", "", Difficulty.EASY, "topic",
                null, "teacher1", payloadWithAnswerCount(3).getAnswers());
        checkEquals("A question must contain exactly 4 answers.",
                QuestionCreateValidator.validate(shortEdit),
                "edit with 3 answers is rejected");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static CreateQuestionData validPayload() {
        return payloadWithCorrectCount(1);
    }

    private static CreateQuestionData payloadWithAnswerCount(int count) {
        List<QuestionAnswer> answers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            answers.add(new QuestionAnswer("Answer " + (i + 1), i == 0));
        }
        return new CreateQuestionData("Question text", "instructions", Difficulty.EASY,
                "topic", null, "CS101", "teacher1", answers);
    }

    private static CreateQuestionData payloadWithCorrectCount(int correctCount) {
        List<QuestionAnswer> answers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            answers.add(new QuestionAnswer("Answer " + (i + 1), i < correctCount));
        }
        return new CreateQuestionData("Question text", "instructions", Difficulty.EASY,
                "topic", null, "CS101", "teacher1", answers);
    }

    private static CreateQuestionData payloadWithBlankAnswer() {
        List<QuestionAnswer> answers = List.of(
                new QuestionAnswer("Answer 1", true),
                new QuestionAnswer("   ", false),
                new QuestionAnswer("Answer 3", false),
                new QuestionAnswer("Answer 4", false)
        );
        return new CreateQuestionData("Question text", "instructions", Difficulty.EASY,
                "topic", null, "CS101", "teacher1", answers);
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
