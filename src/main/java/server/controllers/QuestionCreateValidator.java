package server.controllers;

import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;

import java.util.List;

/**
 * Server-side CREATE QUESTION rules. JavaFX already checks these in the form,
 * but a forged OCSF request can skip the GUI, so the server must reject
 * malformed payloads before any INSERT.
 */
public final class QuestionCreateValidator {

    private QuestionCreateValidator() {
    }

    /**
     * @return an error message if the payload is invalid, or null if it may be inserted
     */
    public static String validate(CreateQuestionData data) {
        if (data == null) {
            return "Question data is required.";
        }
        if (data.getText() == null || data.getText().isBlank()) {
            return "Question text is required.";
        }
        String answersError = validateAnswers(data.getAnswers());
        if (answersError != null) {
            return answersError;
        }
        return com.hsts.shared.model.QuestionIllustration.validate(data.getImageData(), data.getImagePath());
    }

    public static String validate(EditQuestionData data) {
        if (data == null) {
            return "Question data is required.";
        }
        if (data.getQuestionId() == null || data.getQuestionId().isBlank()) {
            return "Question id is required.";
        }
        if (data.getText() == null || data.getText().isBlank()) {
            return "Question text is required.";
        }
        String answersError = validateAnswers(data.getAnswers());
        if (answersError != null) {
            return answersError;
        }
        return com.hsts.shared.model.QuestionIllustration.validate(data.getImageData(), data.getImagePath());
    }

    public static String validateAnswers(List<QuestionAnswer> answers) {
        if (answers == null || answers.size() != 4) {
            return "A question must contain exactly 4 answers.";
        }
        int correctCount = 0;
        for (QuestionAnswer answer : answers) {
            if (answer == null || answer.getText() == null || answer.getText().isBlank()) {
                return "All four answers must be non-empty.";
            }
            if (answer.isCorrect()) {
                correctCount++;
            }
        }
        if (correctCount != 1) {
            return "A question must contain exactly one correct answer.";
        }
        return null;
    }

    /**
     * Same rules for the parallel text/flag lists used by QuestionServerController.
     * A null correctFlags list is the legacy "index 1 is correct" path and counts
     * as exactly one correct answer when there are four texts.
     */
    public static String validateAnswerLists(List<String> answers, List<Integer> correctFlags) {
        if (answers == null || answers.size() != 4) {
            return "A question must contain exactly 4 answers.";
        }
        for (String answer : answers) {
            if (answer == null || answer.isBlank()) {
                return "All four answers must be non-empty.";
            }
        }
        int correctCount = 0;
        if (correctFlags == null) {
            correctCount = 1;
        } else {
            if (correctFlags.size() != 4) {
                return "A question must contain exactly one correct answer.";
            }
            for (Integer flag : correctFlags) {
                if (flag != null && flag == 1) {
                    correctCount++;
                }
            }
        }
        if (correctCount != 1) {
            return "A question must contain exactly one correct answer.";
        }
        return null;
    }
}
