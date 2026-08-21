package com.hsts.shared.model;

/**
 * Client-side course/topic/difficulty matching for Principal Question Bank.
 * Server SQL applies the same optional filters.
 */
public final class PrincipalQuestionFilter {

    private PrincipalQuestionFilter() {
    }

    public static boolean matches(Question question, String courseId, String topic, Difficulty difficulty) {
        if (question == null) {
            return false;
        }
        if (courseId != null && !courseId.isBlank()
                && (question.getCourseId() == null || !courseId.equals(question.getCourseId()))) {
            return false;
        }
        if (topic != null && !topic.isBlank()) {
            String questionTopic = question.getTopic() != null ? question.getTopic() : "";
            if (!questionTopic.toLowerCase().contains(topic.toLowerCase())) {
                return false;
            }
        }
        if (difficulty != null && question.getDifficulty() != difficulty) {
            return false;
        }
        return true;
    }
}
