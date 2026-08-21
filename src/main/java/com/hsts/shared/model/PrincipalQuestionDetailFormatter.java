package com.hsts.shared.model;

import java.util.List;

/**
 * Read-only Principal Question Bank detail text. No JavaFX.
 */
public final class PrincipalQuestionDetailFormatter {

    private PrincipalQuestionDetailFormatter() {
    }

    public static String format(Question question) {
        if (question == null) {
            return "No question selected.";
        }
        StringBuilder text = new StringBuilder();
        text.append("Question ID: ").append(nullToDash(question.getQuestionId())).append('\n');
        text.append("Version: ").append(question.getVersionNumber()).append('\n');
        text.append("Status: ").append(question.versionStatusLabel()).append('\n');
        text.append("Course: ").append(nullToDash(question.getCourseId())).append('\n');
        text.append("Topic: ").append(nullToDash(question.getTopic())).append('\n');
        text.append("Difficulty: ").append(question.getDifficulty() != null ? question.getDifficulty() : "—").append('\n');
        if (question.hasIllustration()) {
            String filename = question.getImagePath();
            text.append("Illustration: ").append(filename != null && !filename.isBlank() ? filename : "attached")
                    .append('\n');
        }
        text.append('\n');
        text.append(nullToDash(question.getText())).append('\n');
        if (question.getInstructions() != null && !question.getInstructions().isBlank()) {
            text.append('\n').append(question.getInstructions()).append('\n');
        }
        text.append('\n');
        List<QuestionAnswer> answers = question.getAnswers();
        String[] letters = {"A", "B", "C", "D"};
        String correctLetter = null;
        if (answers != null) {
            for (int i = 0; i < answers.size() && i < letters.length; i++) {
                QuestionAnswer answer = answers.get(i);
                String answerText = answer != null ? nullToDash(answer.getText()) : "—";
                text.append(letters[i]).append(". ").append(answerText).append('\n');
                if (answer != null && answer.isCorrect()) {
                    correctLetter = letters[i];
                }
            }
        }
        text.append('\n');
        text.append("Correct answer: ").append(correctLetter != null ? correctLetter : "—");
        return text.toString();
    }

    public static String emptyBankMessage() {
        return "No questions available.";
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
