package server.controllers;

import com.hsts.shared.model.Exam;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.dto.GetExamDetailData;
import com.hsts.shared.net.dto.GetExamExecutionsData;
import com.hsts.shared.net.dto.GetExamStatsData;

import java.util.List;

/**
 * Exam-level access for GET_EXAM_STATS / GET_EXAM_DETAIL / GET_EXAM_EXECUTIONS
 * plus teacher-results ownership.
 */
public class ExamResultsAccessTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        Exam owned = new Exam("E1", "CS101", "Midterm", "", List.of(), 60, "teacher1");
        Exam other = new Exam("E2", "CS101", "Other", "", List.of(), 60, "teacher2");
        AuthenticatedSession teacher1 = new AuthenticatedSession("teacher1", "TEACHER");
        AuthenticatedSession teacher2 = new AuthenticatedSession("teacher2", "TEACHER");
        AuthenticatedSession student1 = new AuthenticatedSession("student1", "STUDENT");
        AuthenticatedSession coord1 = new AuthenticatedSession("coord1", "SUBJECT_COORDINATOR");
        AuthenticatedSession principal1 = new AuthenticatedSession("principal1", "PRINCIPAL");

        check(ExamResultsAccess.denyIfNotOwner(owned, "teacher1") == null,
                "authenticated teacher A may view results for teacher A's exam");
        checkEquals(ExamResultsAccess.DENIED,
                ExamResultsAccess.denyIfNotOwner(owned, "teacher2"),
                "authenticated teacher A is rejected for teacher B's exam");
        checkEquals(ExamResultsAccess.DENIED,
                ExamResultsAccess.denyIfNotOwner(owned, null),
                "missing teacher id is rejected");
        checkEquals(ExamResultsAccess.NOT_FOUND,
                ExamResultsAccess.denyIfNotOwner(null, "teacher1"),
                "missing exam is rejected");

        for (Command command : List.of(Command.GET_EXAM_STATS, Command.GET_EXAM_DETAIL,
                Command.GET_EXAM_EXECUTIONS, Command.CREATE_EXAM_EXECUTION, Command.GET_EXECUTION_STATS,
                Command.EXTEND_EXAM_TIME)) {
            check(ExamResultsAccess.denyExamAccess(command, owned, teacher1) == null,
                    "teacher A ALLOW " + command + " for own exam");
            checkEquals(ExamResultsAccess.ACCESS_DENIED,
                    ExamResultsAccess.denyExamAccess(command, owned, teacher2),
                    "teacher A DENY " + command + " for teacher B exam");
        }

        check(ExamResultsAccess.denyExamAccess(Command.GET_EXAM_STATS, other, principal1) == null,
                "principal ALLOW GET_EXAM_STATS for any exam");
        check(ExamResultsAccess.denyExamAccess(Command.GET_EXAM_STATS, null, principal1) == null,
                "principal GET_EXAM_STATS is not blocked when the exam row is missing");

        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.GET_EXAM_STATS, owned, student1),
                "student DENY GET_EXAM_STATS at exam-level helper");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.GET_EXAM_STATS, owned, coord1),
                "coordinator DENY GET_EXAM_STATS at exam-level helper");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.GET_EXAM_DETAIL, owned, student1),
                "student DENY GET_EXAM_DETAIL");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.GET_EXAM_DETAIL, owned, coord1),
                "coordinator DENY GET_EXAM_DETAIL");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.GET_EXAM_DETAIL, owned, principal1),
                "principal DENY GET_EXAM_DETAIL");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.GET_EXAM_EXECUTIONS, owned, student1),
                "student DENY GET_EXAM_EXECUTIONS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.GET_EXAM_EXECUTIONS, owned, principal1),
                "principal DENY GET_EXAM_EXECUTIONS");

        checkEquals("E1",
                ExamResultsAccess.examIdFromPayload(new GetExamDetailData("E1")),
                "GET_EXAM_DETAIL payload has examId only, no actor id");
        checkEquals("E1",
                ExamResultsAccess.examIdFromPayload(new GetExamExecutionsData("E1")),
                "GET_EXAM_EXECUTIONS payload has examId only, no actor id");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.GET_EXECUTION_STATS, owned, student1),
                "student DENY GET_EXECUTION_STATS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.GET_EXECUTION_STATS, owned, principal1),
                "principal DENY GET_EXECUTION_STATS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.CREATE_EXAM_EXECUTION, owned, student1),
                "student DENY CREATE_EXAM_EXECUTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.CREATE_EXAM_EXECUTION, owned, coord1),
                "coordinator DENY CREATE_EXAM_EXECUTION");

        checkEquals("E1",
                ExamResultsAccess.examIdFromPayload(
                        new com.hsts.shared.net.dto.CreateExamExecutionData("E1", "forged", "01-01-2026 09:00", "01-01-2026 10:00", "AB12")),
                "CREATE_EXAM_EXECUTION examId is a resource id");
        checkEquals("EX1",
                ExamResultsAccess.executionIdFromPayload(
                        new com.hsts.shared.net.dto.GetExecutionStatsData("EX1")),
                "GET_EXECUTION_STATS payload has executionId only, no actor id");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.EXTEND_EXAM_TIME, owned, student1),
                "student DENY EXTEND_EXAM_TIME");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.EXTEND_EXAM_TIME, owned, coord1),
                "coordinator DENY EXTEND_EXAM_TIME");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                ExamResultsAccess.denyExamAccess(Command.EXTEND_EXAM_TIME, owned, principal1),
                "principal DENY EXTEND_EXAM_TIME");
        check(ExamResultsAccess.requiresExecutionLookup(Command.EXTEND_EXAM_TIME),
                "EXTEND_EXAM_TIME resolves ownership from executionId, not client examId");
        checkEquals("EX-B",
                ExamResultsAccess.executionIdFromPayload(
                        new com.hsts.shared.net.dto.ExtendExamTimeData("E-FORGED", "EX-B", "teacher2", 10)),
                "EXTEND_EXAM_TIME executionId is a resource id; examId/teacherId are not used for access");

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

    private static void checkEquals(String expected, String actual, String description) {
        check(expected != null && expected.equals(actual), description + " (got: " + actual + ")");
    }
}
