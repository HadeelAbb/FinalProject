package server.controllers;

import java.time.LocalDateTime;

/**
 * Focused, non-destructive checks for the in-memory active-exam lock and
 * personal timer continuity. Does not connect to MySQL, OCSF, or Groq.
 */
public class ActiveExamTrackerTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        ActiveExamTracker tracker = new ActiveExamTracker();
        LocalDateTime t0 = LocalDateTime.of(2026, 8, 18, 12, 0, 0);

        check(!tracker.isActiveInCourse("student1", "CS101"),
                "student1 with no sitting is not active in CS101");

        tracker.markStarted("student1", "CS101", "E61202", "EX1", t0);
        ActiveExamTracker.ActiveSitting sitting = tracker.getSitting("student1", "E61202");
        check(sitting != null && t0.equals(sitting.getStartedAt()),
                "start at T0 → startedAt = T0");
        check(sitting != null && "EX1".equals(sitting.getExecutionId()),
                "sitting stores execution id EX1");

        LocalDateTime tLater = t0.plusMinutes(20);
        tracker.markStarted("student1", "CS101", "E61202", "EX1", tLater);
        sitting = tracker.getSitting("student1", "E61202");
        check(sitting != null && t0.equals(sitting.getStartedAt()),
                "same student/execution START again later → startedAt still T0");
        check(sitting != null && "EX1".equals(sitting.getExecutionId()),
                "resume preserves the same execution identity");

        int remainingAt10 = ActiveExamTracker.remainingSeconds(t0, 60, 0, t0.plusMinutes(10));
        int remainingAt20 = ActiveExamTracker.remainingSeconds(t0, 60, 0, tLater);
        check(remainingAt10 == 50 * 60, "after 10 minutes, 50 minutes remain");
        check(remainingAt20 == 40 * 60, "after 20 minutes, 40 minutes remain");
        check(remainingAt20 < remainingAt10,
                "remaining time decreases rather than resets");
        check(remainingAt20 != 60 * 60,
                "resume does not grant a fresh 60-minute duration");

        check(tracker.isActiveInCourse("student1", "CS101"),
                "start → disconnect → CS101 must STILL be blocked");
        check(t0.equals(tracker.getSitting("student1", "E61202").getStartedAt()),
                "logout/disconnect → startedAt remains T0");
        check(!tracker.isActiveInCourse("student1", "MATH201"),
                "other course unaffected");
        check(!tracker.isActiveInCourse("student2", "CS101"),
                "other student unaffected");

        int withExtraAtStart = ActiveExamTracker.remainingSeconds(t0, 60, 10, t0);
        int withExtraAfter10 = ActiveExamTracker.remainingSeconds(t0, 60, 10, t0.plusMinutes(10));
        check(withExtraAtStart == 70 * 60,
                "extra minutes are included in allowed duration");
        check(withExtraAfter10 == 60 * 60,
                "resume remaining includes current extra minutes");

        check(ActiveExamTracker.remainingSeconds(t0, 60, 0, t0.plusMinutes(60)) == 0,
                "exactly at expiry remaining is 0, not a fresh duration");
        check(ActiveExamTracker.remainingSeconds(t0, 60, 0, t0.plusMinutes(90)) == 0,
                "expired sitting does not receive fresh full duration");
        check(ActiveExamTracker.remainingSeconds(t0, 60, 10, t0.plusMinutes(70)) == 0,
                "expiry accounts for extra minutes (70 of 70 used)");
        check(ActiveExamTracker.remainingSeconds(t0, 60, 10, t0.plusMinutes(65)) == 5 * 60,
                "5 minutes remain when extra time is still unused");

        tracker.clearByExam("student1", "E61202");
        check(tracker.getSitting("student1", "E61202") == null,
                "successful submit → sitting removed");
        check(!tracker.isActiveInCourse("student1", "CS101"),
                "successful submit → CS101 unlocked");

        tracker.markStarted("student1", "CS101", "E61202", "EX1", t0);
        tracker.markStarted("student1", "CS101", "E61202", "EX1", tLater);
        check(t0.equals(tracker.getSitting("student1", "E61202").getStartedAt()),
                "failed-submit path: startedAt is not reset");

        tracker.markStarted("student2", "CS101", "E61202", "EX1", tLater);
        check(t0.equals(tracker.getSitting("student1", "E61202").getStartedAt()),
                "different student independent — student1 startedAt unchanged");
        check(tLater.equals(tracker.getSitting("student2", "E61202").getStartedAt()),
                "different student independent — student2 has own startedAt");

        LocalDateTime tExec2 = t0.plusHours(1);
        tracker.markStarted("student1", "CS101", "E61202", "EX2", tExec2);
        sitting = tracker.getSitting("student1", "E61202");
        check(sitting != null && "EX2".equals(sitting.getExecutionId()),
                "different execution is a different sitting");
        check(sitting != null && tExec2.equals(sitting.getStartedAt()),
                "different execution does not reuse EX1 startedAt");

        tracker.markStarted("student1", "MATH201", "E90001", "EXMATH1", t0);
        check(tracker.isActiveInCourse("student1", "MATH201"),
                "MATH201 sitting is independent of CS101");
        tracker.clearByExam("student1", "E61202");
        check(tracker.isActiveInCourse("student1", "MATH201"),
                "clearing CS101 exam does not clear MATH201");

        tracker.markStarted(null, "CS101", "E1", "EX1", t0);
        tracker.markStarted("student1", null, "E1", "EX1", t0);
        tracker.markStarted("student1", "CS101", null, "EX1", t0);
        check(tracker.getSitting("student1", "E1") == null,
                "blank identity/course/exam does not create a sitting");

        checkEquals(ActiveExamTracker.BOT_UNAVAILABLE_MESSAGE,
                "Study Bot is unavailable while you are taking an active exam in this course.",
                "server failure message is the required active-exam text");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  OK  - " + description);
        } else {
            System.out.println("FAIL  - " + description);
            failCount++;
        }
    }

    private static void checkEquals(String actual, String expected, String description) {
        check(expected.equals(actual), description + " (got: " + actual + ")");
    }
}
