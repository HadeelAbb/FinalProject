package server.controllers;

import com.hsts.shared.model.Exam;
import com.hsts.shared.model.Question;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lineage rules for Exam versions. No JavaFX.
 * Physical exam_id is immutable per version; rootExamId groups the lineage.
 */
public final class ExamVersioning {

    public static final String HISTORICAL_NOT_EDITABLE =
            "Only the current version of this exam can be edited.";
    public static final String HISTORICAL_NOT_SUBMITTABLE =
            "Only the current version of this exam can be submitted for approval.";
    public static final String NEW_QUESTION_MUST_BE_CURRENT =
            "Newly added questions must be the current version in the question bank.";
    public static final String TITLE_REQUIRED = "An exam title is required.";
    public static final String DURATION_INVALID = "Duration must be at least 1 minute.";
    public static final String SOURCE_NOT_FOUND = "Exam not found.";

    private ExamVersioning() {
    }

    public static int nextVersionNumber(int currentMaxVersion) {
        if (currentMaxVersion < 1) {
            return 1;
        }
        return currentMaxVersion + 1;
    }

    public static String rootOf(Exam exam) {
        if (exam == null) {
            return null;
        }
        return exam.getRootExamId();
    }

    public static boolean isCurrent(Exam exam) {
        return exam != null && exam.isLatest();
    }

    public static List<Exam> currentVersionsOnly(List<Exam> exams) {
        List<Exam> latest = new ArrayList<>();
        if (exams == null) {
            return latest;
        }
        for (Exam exam : exams) {
            if (isCurrent(exam)) {
                latest.add(exam);
            }
        }
        return latest;
    }

    public static Set<String> physicalQuestionIds(Exam exam) {
        Set<String> ids = new HashSet<>();
        if (exam == null || exam.getQuestions() == null) {
            return ids;
        }
        for (Question question : exam.getQuestions()) {
            if (question != null && question.getQuestionId() != null) {
                ids.add(question.getQuestionId());
            }
        }
        return ids;
    }

    /**
     * Carried-forward questions from the source exam may be historical bank
     * versions. Newly selected ids must be current question-bank versions.
     */
    public static String validateNewlyAddedQuestionsAreLatest(Collection<String> sourceQuestionIds,
                                                              List<Question> requestedQuestions) {
        Set<String> source = sourceQuestionIds != null
                ? new HashSet<>(sourceQuestionIds) : new HashSet<>();
        if (requestedQuestions == null) {
            return null;
        }
        for (Question question : requestedQuestions) {
            if (question == null || question.getQuestionId() == null) {
                continue;
            }
            if (!source.contains(question.getQuestionId()) && !question.isLatest()) {
                return NEW_QUESTION_MUST_BE_CURRENT;
            }
        }
        return null;
    }

    public static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            return TITLE_REQUIRED;
        }
        return null;
    }

    public static String validateDuration(int durationMinutes) {
        if (durationMinutes < 1) {
            return DURATION_INVALID;
        }
        return null;
    }
}
