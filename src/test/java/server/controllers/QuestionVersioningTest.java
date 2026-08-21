package server.controllers;

import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;

import java.util.ArrayList;
import java.util.List;

/**
 * Focused lineage/version checks. No MySQL, OCSF, or JavaFX.
 */
public class QuestionVersioningTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        Question v1 = question("Q1", "Original", "B", true);
        List<Question> bank = new ArrayList<>();
        bank.add(v1);

        Question v2 = nextVersion(v1, "Updated", "C");
        v1.setLatest(false);
        bank.add(v2);

        check(bank.stream().anyMatch(q -> "Q1".equals(q.getQuestionId()) && "Original".equals(q.getText())),
                "first edit preserves v1 with original text");
        check(bank.stream().anyMatch(q -> "Updated".equals(q.getText()) && q.getVersionNumber() == 2),
                "v2 exists with updated text");
        check(!v1.isLatest() && v2.isLatest(), "v2 latest, v1 historical");

        check(correctLetter(v1).equals("B") && correctLetter(v2).equals("C"),
                "v1 still has correct B and v2 has correct C");

        Question v3 = nextVersion(v2, "Updated again", "A");
        v2.setLatest(false);
        bank.add(v3);
        check(bank.size() == 3, "multiple edits keep v1/v2/v3");
        check(v1.getVersionNumber() == 1 && v2.getVersionNumber() == 2 && v3.getVersionNumber() == 3,
                "version numbers are 1/2/3");
        check(QuestionVersioning.currentVersionsOnly(bank).size() == 1
                        && QuestionVersioning.currentVersionsOnly(bank).get(0).getVersionNumber() == 3,
                "only v3 is current");
        check(QuestionVersioning.distinctLineages(bank) == 1,
                "v1/v2/v3 are one logical question, not three");

        List<Question> picker = QuestionVersioning.currentVersionsOnly(bank);
        check(picker.size() == 1 && picker.get(0).getQuestionId().equals(v3.getQuestionId()),
                "manual/automatic selection uses latest only");
        check(picker.stream().noneMatch(q -> "Q1".equals(q.getQuestionId())),
                "historical v1 is not a new-exam candidate");

        check(QuestionVersioning.nextVersionNumber(1) == 2, "next after v1 is 2");
        check(QuestionVersioning.nextVersionNumber(2) == 3, "next after v2 is 3");

        checkEquals(QuestionVersioning.HISTORICAL_NOT_EDITABLE,
                v1.isLatest() ? null : QuestionVersioning.HISTORICAL_NOT_EDITABLE,
                "historical version is not editable");

        com.hsts.shared.model.Exam examA = new com.hsts.shared.model.Exam(
                "EA", "CS101", "Exam A", "", List.of(v1), 60, "teacher1");
        check(QuestionVersioning.isReferencedByExam("Q1", List.of(examA)),
                "v1 used by Exam A is treated as referenced");
        check(!QuestionVersioning.isReferencedByExam(v2.getQuestionId(), List.of(examA)),
                "unused historical/current sibling is not referenced by Exam A");
        com.hsts.shared.model.Exam examB = new com.hsts.shared.model.Exam(
                "EB", "CS101", "Exam B", "", List.of(v2), 60, "teacher1");
        check(QuestionVersioning.isReferencedByExam(v2.getQuestionId(), List.of(examB)),
                "current version used by Exam B is treated as referenced");
        check(!QuestionVersioning.isReferencedByExam("Q1", List.of()),
                "unused version is not referenced");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static Question nextVersion(Question current, String text, String correctLetter) {
        Question next = question("Q" + QuestionVersioning.nextVersionNumber(current.getVersionNumber()),
                text, correctLetter, true);
        next.setRootQuestionId(current.getRootQuestionId());
        next.setVersionNumber(QuestionVersioning.nextVersionNumber(current.getVersionNumber()));
        next.setLatest(true);
        return next;
    }

    private static Question question(String id, String text, String correctLetter, boolean latest) {
        List<QuestionAnswer> answers = List.of(
                new QuestionAnswer("A3", "A".equals(correctLetter)),
                new QuestionAnswer("B4", "B".equals(correctLetter)),
                new QuestionAnswer("C5", "C".equals(correctLetter)),
                new QuestionAnswer("D6", "D".equals(correctLetter)));
        Question q = new Question(id, text, "", Difficulty.EASY, "topic", null, "CS101", answers);
        q.setLatest(latest);
        return q;
    }

    private static String correctLetter(Question question) {
        List<QuestionAnswer> answers = question.getAnswers();
        String[] letters = {"A", "B", "C", "D"};
        for (int i = 0; i < answers.size() && i < letters.length; i++) {
            if (answers.get(i).isCorrect()) {
                return letters[i];
            }
        }
        return "?";
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
