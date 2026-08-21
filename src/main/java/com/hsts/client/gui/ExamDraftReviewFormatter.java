package com.hsts.client.gui;

import com.hsts.shared.model.Exam;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only text for the teacher draft review. Uses each question's assigned
 * exam points; does not recompute 100 / n.
 */
public final class ExamDraftReviewFormatter {

    private ExamDraftReviewFormatter() {
    }

    public static List<String> formatQuestions(Exam exam) {
        List<String> blocks = new ArrayList<>();
        if (exam == null || exam.getQuestions() == null) {
            return blocks;
        }
        for (Question question : exam.getQuestions()) {
            if (question != null) {
                blocks.add(formatQuestion(question));
            }
        }
        return blocks;
    }

    public static String formatQuestion(Question question) {
        StringBuilder text = new StringBuilder();
        if (question.getQuestionId() != null && !question.getQuestionId().isBlank()) {
            text.append(question.getQuestionId()).append('\n');
        }
        if (question.getText() != null && !question.getText().isBlank()) {
            text.append(question.getText()).append('\n');
        }
        String topicDifficulty = topicDifficultyLine(question);
        if (!topicDifficulty.isEmpty()) {
            text.append(topicDifficulty).append('\n');
        }
        if (question.getAnswers() != null) {
            for (QuestionAnswer answer : question.getAnswers()) {
                if (answer != null && answer.getText() != null && !answer.getText().isBlank()) {
                    text.append("  - ").append(answer.getText()).append('\n');
                }
            }
        }
        text.append(question.getPoints()).append(" points");
        return text.toString();
    }

    public static int sumAssignedPoints(Exam exam) {
        int total = 0;
        if (exam == null || exam.getQuestions() == null) {
            return 0;
        }
        for (Question question : exam.getQuestions()) {
            if (question != null) {
                total += question.getPoints();
            }
        }
        return total;
    }

    public static String formatTotal(Exam exam) {
        return "Total: " + sumAssignedPoints(exam) + " / 100";
    }

    public static String formatMeta(Exam exam) {
        if (exam == null) {
            return "";
        }
        String status = exam.getStatus() != null ? exam.getStatus().name() : "";
        return "Course: " + nullToEmpty(exam.getCourseId())
                + "    Duration: " + exam.getDurationMinutes() + " min"
                + "    Status: " + status;
    }

    private static String topicDifficultyLine(Question question) {
        boolean hasTopic = question.getTopic() != null && !question.getTopic().isBlank();
        boolean hasDifficulty = question.getDifficulty() != null;
        if (hasTopic && hasDifficulty) {
            return question.getTopic() + " / " + question.getDifficulty();
        }
        if (hasTopic) {
            return question.getTopic();
        }
        if (hasDifficulty) {
            return question.getDifficulty().name();
        }
        return "";
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
