package server.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, student-specific record of an exam sitting that has started
 * but has not yet been submitted. Production MySQL has no in-progress
 * attempt row (exam_answers is written only at SUBMIT), so this is the
 * smallest server-side way to know a student is currently taking an exam
 * and when that student's personal timer began.
 *
 * Keyed per student + exam (not globally, and not by "execution window open").
 * Resume is per execution: a second START of the SAME execution keeps the
 * original startedAt. A different execution is a different sitting.
 *
 * Sittings survive logout and disconnect. The only production clear is a
 * successful SUBMIT_EXAM (clearByExam).
 */
public class ActiveExamTracker {

    public static final String BOT_UNAVAILABLE_MESSAGE =
            "Study Bot is unavailable while you are taking an active exam in this course.";

    public static final class ActiveSitting {
        private final String studentId;
        private final String courseId;
        private final String examId;
        private final String executionId;
        private final LocalDateTime startedAt;

        public ActiveSitting(String studentId, String courseId, String examId, String executionId,
                             LocalDateTime startedAt) {
            this.studentId = studentId;
            this.courseId = courseId;
            this.examId = examId;
            this.executionId = executionId;
            this.startedAt = startedAt;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getCourseId() {
            return courseId;
        }

        public String getExamId() {
            return examId;
        }

        public String getExecutionId() {
            return executionId;
        }

        public LocalDateTime getStartedAt() {
            return startedAt;
        }
    }

    private final ConcurrentHashMap<String, ActiveSitting> sittings = new ConcurrentHashMap<>();

    private static String sittingKey(String studentId, String examId) {
        return studentId + "\0" + examId;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public ActiveSitting markStarted(String studentId, String courseId, String examId, String executionId) {
        return markStarted(studentId, courseId, examId, executionId, LocalDateTime.now());
    }

    /**
     * Records a new sitting, or resumes the existing one when the same student
     * starts the same execution again. Resume does not replace startedAt.
     */
    public ActiveSitting markStarted(String studentId, String courseId, String examId, String executionId,
                                     LocalDateTime startedAt) {
        if (isBlank(studentId) || isBlank(courseId) || isBlank(examId) || startedAt == null) {
            return null;
        }
        String key = sittingKey(studentId, examId);
        ActiveSitting existing = sittings.get(key);
        if (existing != null && executionId != null && executionId.equals(existing.getExecutionId())) {
            return existing;
        }
        ActiveSitting created = new ActiveSitting(studentId, courseId, examId, executionId, startedAt);
        sittings.put(key, created);
        return created;
    }

    public void clearByExam(String studentId, String examId) {
        if (isBlank(studentId) || isBlank(examId)) {
            return;
        }
        sittings.remove(sittingKey(studentId, examId));
    }

    public boolean isActiveInCourse(String studentId, String courseId) {
        if (isBlank(studentId) || isBlank(courseId)) {
            return false;
        }
        for (ActiveSitting sitting : sittings.values()) {
            if (studentId.equals(sitting.getStudentId()) && courseId.equals(sitting.getCourseId())) {
                return true;
            }
        }
        return false;
    }

    public ActiveSitting getSitting(String studentId, String examId) {
        if (isBlank(studentId) || isBlank(examId)) {
            return null;
        }
        return sittings.get(sittingKey(studentId, examId));
    }

    /**
     * Personal remaining seconds for a sitting.
     * remaining = (base duration + extra minutes) * 60 - elapsed since original startedAt.
     * Never uses ExamExecution.scheduled_start. Never returns a fresh full duration
     * after time has already elapsed; expired sittings return 0.
     */
    public static int remainingSeconds(LocalDateTime startedAt, int durationMinutes, int extraMinutes,
                                       LocalDateTime now) {
        if (startedAt == null || now == null) {
            return 0;
        }
        long allowedSeconds = (Math.max(0, durationMinutes) + Math.max(0, extraMinutes)) * 60L;
        long elapsedSeconds = Duration.between(startedAt, now).getSeconds();
        if (elapsedSeconds < 0) {
            elapsedSeconds = 0;
        }
        long remaining = allowedSeconds - elapsedSeconds;
        if (remaining <= 0) {
            return 0;
        }
        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }
}
