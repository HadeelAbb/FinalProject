package server.controllers;

import com.hsts.shared.model.Question;

import java.util.ArrayList;
import java.util.List;

/**
 * Lineage rules for Question Bank versions. No JavaFX.
 * Physical question_id is immutable per version; rootQuestionId groups the lineage.
 */
public final class QuestionVersioning {

    public static final String HISTORICAL_NOT_EDITABLE =
            "Only the current version of this question can be edited.";
    public static final String USED_BY_EXAM =
            "This question version is used by an exam and cannot be deleted.";

    private QuestionVersioning() {
    }

    public static int nextVersionNumber(int currentMaxVersion) {
        if (currentMaxVersion < 1) {
            return 1;
        }
        return currentMaxVersion + 1;
    }

    public static String rootOf(Question question) {
        if (question == null) {
            return null;
        }
        return question.getRootQuestionId();
    }

    public static boolean isCurrent(Question question) {
        return question != null && question.isLatest();
    }

    public static List<Question> currentVersionsOnly(List<Question> questions) {
        List<Question> latest = new ArrayList<>();
        if (questions == null) {
            return latest;
        }
        for (Question question : questions) {
            if (isCurrent(question)) {
                latest.add(question);
            }
        }
        return latest;
    }

    public static int distinctLineages(List<Question> questions) {
        java.util.LinkedHashSet<String> roots = new java.util.LinkedHashSet<>();
        if (questions == null) {
            return 0;
        }
        for (Question question : questions) {
            if (question != null && question.getRootQuestionId() != null) {
                roots.add(question.getRootQuestionId());
            }
        }
        return roots.size();
    }

    /** True if any exam still references this physical question/version id. */
    public static boolean isReferencedByExam(String questionId, List<com.hsts.shared.model.Exam> exams) {
        if (questionId == null || questionId.isBlank() || exams == null) {
            return false;
        }
        for (com.hsts.shared.model.Exam exam : exams) {
            if (exam == null || exam.getQuestions() == null) {
                continue;
            }
            for (Question question : exam.getQuestions()) {
                if (question != null && questionId.equals(question.getQuestionId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
