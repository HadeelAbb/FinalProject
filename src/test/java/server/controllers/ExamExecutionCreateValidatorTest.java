package server.controllers;

import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Focused, non-destructive checks for ExamExecution create rules.
 * Does not connect to MySQL or send OCSF traffic.
 */
public class ExamExecutionCreateValidatorTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        check(ExamExecutionCreateValidator.validateApproved(ExamStatus.APPROVED) == null,
                "APPROVED exam is allowed");
        checkEquals("Only approved exams can be executed.",
                ExamExecutionCreateValidator.validateApproved(ExamStatus.DRAFT),
                "DRAFT exam is rejected");
        checkEquals("Only approved exams can be executed.",
                ExamExecutionCreateValidator.validateApproved(ExamStatus.PENDING_APPROVAL),
                "PENDING_APPROVAL exam is rejected");
        checkEquals("Only approved exams can be executed.",
                ExamExecutionCreateValidator.validateApproved(ExamStatus.REJECTED),
                "REJECTED exam is rejected");
        check(ExamExecutionCreateValidator.validateApproved(ExamStatus.APPROVED) == null
                        && ExamResultsAccess.denyIfNotOwner(
                                exam("teacher1", ExamStatus.APPROVED), "teacher1") == null,
                "teacher A may create an execution of their own APPROVED exam");
        checkEquals(ExamResultsAccess.DENIED,
                ExamResultsAccess.denyIfNotOwner(
                        exam("teacher2", ExamStatus.APPROVED), "teacher1"),
                "teacher A cannot create an execution of teacher B's APPROVED exam");
        checkEquals("Only approved exams can be executed.",
                ExamExecutionCreateValidator.validateApproved(ExamStatus.DRAFT),
                "teacher A cannot execute their own DRAFT exam");

        LocalDateTime open = LocalDateTime.of(2026, 8, 16, 15, 0);
        LocalDateTime close = LocalDateTime.of(2026, 8, 16, 17, 0);
        check(ExamExecutionCreateValidator.validateWindow(open, close) == null,
                "close after open is accepted");
        checkEquals("Closing time must be after opening time.",
                ExamExecutionCreateValidator.validateWindow(open, open),
                "equal open/close is rejected");
        checkEquals("Closing time must be after opening time.",
                ExamExecutionCreateValidator.validateWindow(close, open),
                "close before open is rejected");
        checkEquals("Opening and closing times are required.",
                ExamExecutionCreateValidator.validateWindow(null, close),
                "missing open time is rejected");

        check(ExamExecutionCreateValidator.validateExecutionCode("HKD6") == null,
                "4-character alphanumeric code is accepted");
        check(ExamExecutionCreateValidator.validateExecutionCode("A3F9") == null,
                "mixed alphanumeric code is accepted");
        checkEquals("Execution code must contain exactly 4 characters.",
                ExamExecutionCreateValidator.validateExecutionCode("ABC"),
                "3-character code is rejected");
        checkEquals("Execution code must contain exactly 4 characters.",
                ExamExecutionCreateValidator.validateExecutionCode("ABCDE"),
                "5-character code is rejected");
        checkEquals("Execution code must contain exactly 4 characters.",
                ExamExecutionCreateValidator.validateExecutionCode("  "),
                "blank code is rejected");
        checkEquals("Execution code may contain only A-Z and 0-9.",
                ExamExecutionCreateValidator.validateExecutionCode("A@12"),
                "non-alphanumeric code is rejected");
        check(ExamExecutionCreateValidator.validateExecutionCode(" ab12 ") == null,
                "trimmed lowercase code is accepted");
        checkEquals("AB12",
                ExamExecutionCreateValidator.normalizeExecutionCode(" ab12 "),
                "code is trimmed and uppercased");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static Exam exam(String teacherId, ExamStatus status) {
        Exam exam = new Exam("E1", "CS101", "Midterm", "", List.of(), 60, teacherId);
        exam.setStatus(status);
        return exam;
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
