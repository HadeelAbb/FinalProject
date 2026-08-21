package server.controllers;

import com.hsts.shared.model.Question;

import java.util.List;
import java.util.Map;

/**
 * Per-question exam points: each value must be a positive integer and the
 * assignment for one exam must total exactly 100. Does not silently rescale.
 */
public final class ExamQuestionPointsValidator {

    public static final int REQUIRED_TOTAL = 100;

    private ExamQuestionPointsValidator() {
    }

    public static String validate(List<String> questionIds, Map<String, Integer> pointsByQuestionId) {
        if (questionIds == null || questionIds.isEmpty()) {
            return "An exam must contain at least one question.";
        }
        if (pointsByQuestionId == null) {
            return "Each exam question must have points assigned.";
        }
        int total = 0;
        for (String questionId : questionIds) {
            if (questionId == null || questionId.isBlank()) {
                return "Each exam question must have points assigned.";
            }
            Integer points = pointsByQuestionId.get(questionId);
            String pointError = validateSingle(points);
            if (pointError != null) {
                return pointError;
            }
            total += points;
        }
        if (pointsByQuestionId.size() < questionIds.size()) {
            return "Each exam question must have points assigned.";
        }
        return validateTotal(total);
    }

    public static String validateQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return "An exam must contain at least one question.";
        }
        int total = 0;
        for (Question question : questions) {
            String pointError = validateSingle(question != null ? question.getPoints() : null);
            if (pointError != null) {
                return pointError;
            }
            total += question.getPoints();
        }
        return validateTotal(total);
    }

    public static String validateEqualSplit(int questionCount) {
        if (questionCount <= 0) {
            return "An exam must contain at least one question.";
        }
        if (REQUIRED_TOTAL % questionCount != 0) {
            return "Automatic exams need a question count that divides 100 evenly so each question "
                    + "can receive equal integer points. Current count: " + questionCount + ".";
        }
        return null;
    }

    public static int equalSplitPoints(int questionCount) {
        if (questionCount <= 0 || REQUIRED_TOTAL % questionCount != 0) {
            return 0;
        }
        return REQUIRED_TOTAL / questionCount;
    }

    public static String validateSingle(Integer points) {
        if (points == null) {
            return "Each question's points must be greater than 0.";
        }
        if (points <= 0) {
            return "Each question's points must be greater than 0.";
        }
        return null;
    }

    public static String validateTotal(int total) {
        if (total != REQUIRED_TOTAL) {
            return "Exam question points must total exactly 100. Current total: " + total + ".";
        }
        return null;
    }

    /**
     * Weighted auto-grade using official exam-question points only.
     * selectedAnswers / client data must not supply point values.
     */
    public static int grade(List<Question> examQuestions, Map<String, String> selectedAnswers,
                            Map<String, String> officialCorrectAnswers) {
        if (examQuestions == null || examQuestions.isEmpty()) {
            return 0;
        }
        int earned = 0;
        for (Question question : examQuestions) {
            if (question == null || question.getQuestionId() == null) {
                continue;
            }
            String selected = selectedAnswers != null ? selectedAnswers.get(question.getQuestionId()) : null;
            String correct = officialCorrectAnswers != null
                    ? officialCorrectAnswers.get(question.getQuestionId()) : null;
            if (selected != null && correct != null
                    && selected.trim().equalsIgnoreCase(correct.trim())) {
                earned += Math.max(0, question.getPoints());
            }
        }
        return earned;
    }
}
