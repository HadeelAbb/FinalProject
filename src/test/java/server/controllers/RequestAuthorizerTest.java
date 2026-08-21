package server.controllers;

import com.hsts.shared.net.Command;

/**
 * Focused authorization-gate checks. No MySQL, OCSF, or Groq.
 */
public class RequestAuthorizerTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        AuthenticatedSession student1 = new AuthenticatedSession("student1", "STUDENT");
        AuthenticatedSession student2 = new AuthenticatedSession("student2", "STUDENT");
        AuthenticatedSession teacher1 = new AuthenticatedSession("teacher1", "TEACHER");
        AuthenticatedSession coord1 = new AuthenticatedSession("coord1", "SUBJECT_COORDINATOR");
        AuthenticatedSession principal1 = new AuthenticatedSession("principal1", "PRINCIPAL");

        check(RequestAuthorizer.authorize(Command.LOGIN, null) == null,
                "LOGIN is allowed without a session");
        check(RequestAuthorizer.authorize(Command.LOGOUT, null) == null,
                "LOGOUT is allowed without a session");

        checkEquals(RequestAuthorizer.AUTH_REQUIRED,
                RequestAuthorizer.authorize(Command.START_EXAM, null),
                "unauthenticated START_EXAM is blocked");
        checkEquals(RequestAuthorizer.AUTH_REQUIRED,
                RequestAuthorizer.authorize(Command.APPROVE_EXAM, null),
                "unauthenticated APPROVE_EXAM is blocked");
        checkEquals(RequestAuthorizer.AUTH_REQUIRED,
                RequestAuthorizer.authorize(Command.CONFIRM_GRADE, null),
                "unauthenticated CONFIRM_GRADE is blocked");
        checkEquals(RequestAuthorizer.AUTH_REQUIRED,
                RequestAuthorizer.authorize(Command.CREATE_QUESTION, null),
                "unauthenticated CREATE_QUESTION is blocked");
        checkEquals(RequestAuthorizer.AUTH_REQUIRED,
                RequestAuthorizer.authorize(Command.ASK_BOT_QUESTION, null),
                "unauthenticated ASK_BOT_QUESTION is blocked");

        check(RequestAuthorizer.authorize(Command.START_EXAM, student1) == null,
                "same-user student may START_EXAM");
        check(RequestAuthorizer.authorize(Command.SUBMIT_EXAM, student1) == null,
                "same-user student may SUBMIT_EXAM");
        check(RequestAuthorizer.authorize(Command.GET_MY_RESULTS, student1) == null,
                "same-user student may GET_MY_RESULTS");
        check(RequestAuthorizer.authorize(Command.GET_EXAM_ANSWER_COPY, student1) == null,
                "same-user student may GET_EXAM_ANSWER_COPY");
        check(RequestAuthorizer.authorize(Command.ASK_BOT_QUESTION, student1) == null,
                "same-user student may ASK_BOT_QUESTION");

        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.START_EXAM, teacher1),
                "teacher cannot START_EXAM");
        checkEquals(RequestAuthorizer.OWN_RESULTS_ONLY,
                RequestAuthorizer.authorize(Command.GET_MY_RESULTS, teacher1),
                "teacher cannot GET_MY_RESULTS as a student");
        checkEquals(RequestAuthorizer.ONLY_COORDINATORS,
                RequestAuthorizer.authorize(Command.APPROVE_EXAM, student1),
                "student cannot APPROVE_EXAM");
        checkEquals(RequestAuthorizer.ONLY_COORDINATORS,
                RequestAuthorizer.authorize(Command.APPROVE_EXAM, teacher1),
                "teacher cannot APPROVE_EXAM even with a forged coordinatorId");
        checkEquals(RequestAuthorizer.ONLY_COORDINATORS,
                RequestAuthorizer.authorize(Command.REJECT_EXAM, teacher1),
                "teacher cannot REJECT_EXAM");
        check(RequestAuthorizer.authorize(Command.APPROVE_EXAM, coord1) == null,
                "legitimate coordinator may APPROVE_EXAM");
        check(RequestAuthorizer.authorize(Command.REJECT_EXAM, coord1) == null,
                "legitimate coordinator may REJECT_EXAM");

        checkEquals(RequestAuthorizer.ONLY_TEACHERS_GRADE,
                RequestAuthorizer.authorize(Command.CONFIRM_GRADE, coord1),
                "coordinator cannot CONFIRM_GRADE");
        checkEquals(RequestAuthorizer.ONLY_TEACHERS_GRADE,
                RequestAuthorizer.authorize(Command.CONFIRM_GRADE, student1),
                "student cannot CONFIRM_GRADE");
        checkEquals(RequestAuthorizer.ONLY_TEACHERS_GRADE,
                RequestAuthorizer.authorize(Command.CONFIRM_GRADE, principal1),
                "principal cannot CONFIRM_GRADE");
        check(RequestAuthorizer.authorize(Command.CONFIRM_GRADE, teacher1) == null,
                "legitimate teacher may CONFIRM_GRADE");
        check(RequestAuthorizer.authorize(Command.GET_PENDING_GRADING, teacher1) == null,
                "legitimate teacher may GET_PENDING_GRADING");
        check(RequestAuthorizer.authorize(Command.GET_EXAM_RESULTS, teacher1) == null,
                "legitimate teacher may GET_EXAM_RESULTS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_EXAM_RESULTS, student1),
                "student cannot GET_EXAM_RESULTS");
        check(RequestAuthorizer.authorize(Command.GET_EXAM_DETAIL, teacher1) == null,
                "teacher may GET_EXAM_DETAIL");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_EXAM_DETAIL, student1),
                "student cannot GET_EXAM_DETAIL");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_EXAM_DETAIL, principal1),
                "principal cannot GET_EXAM_DETAIL");
        check(RequestAuthorizer.authorize(Command.GET_EXAM_EXECUTIONS, teacher1) == null,
                "teacher may GET_EXAM_EXECUTIONS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_EXAM_EXECUTIONS, student1),
                "student cannot GET_EXAM_EXECUTIONS");
        check(RequestAuthorizer.authorize(Command.GET_EXAM_STATS, teacher1) == null,
                "teacher role may GET_EXAM_STATS");
        check(RequestAuthorizer.authorize(Command.GET_EXAM_STATS, principal1) == null,
                "principal may GET_EXAM_STATS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_EXAM_STATS, student1),
                "student cannot GET_EXAM_STATS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_EXAM_STATS, coord1),
                "coordinator cannot GET_EXAM_STATS");

        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_QUESTION, student1),
                "non-teacher cannot CREATE_QUESTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.EDIT_QUESTION, student1),
                "non-teacher cannot EDIT_QUESTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.DELETE_QUESTION, student1),
                "student cannot DELETE_QUESTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.DELETE_QUESTION, coord1),
                "coordinator cannot DELETE_QUESTION");
        check(RequestAuthorizer.authorize(Command.CREATE_QUESTION, teacher1) == null,
                "teacher may CREATE_QUESTION");
        check(RequestAuthorizer.authorize(Command.EDIT_QUESTION, teacher1) == null,
                "teacher may EDIT_QUESTION");
        check(RequestAuthorizer.authorize(Command.DELETE_QUESTION, teacher1) == null,
                "teacher may DELETE_QUESTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_QUESTION, principal1),
                "principal cannot CREATE_QUESTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.EDIT_QUESTION, principal1),
                "principal cannot EDIT_QUESTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.DELETE_QUESTION, principal1),
                "principal cannot DELETE_QUESTION");

        check(RequestAuthorizer.authorize(Command.GET_ALL_QUESTIONS, principal1) == null,
                "principal may GET_ALL_QUESTIONS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_ALL_QUESTIONS, teacher1),
                "teacher cannot GET_ALL_QUESTIONS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_ALL_QUESTIONS, student1),
                "student cannot GET_ALL_QUESTIONS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_ALL_QUESTIONS, coord1),
                "coordinator cannot GET_ALL_QUESTIONS");

        check(RequestAuthorizer.authorize(Command.CREATE_EXAM_MANUAL, teacher1) == null,
                "teacher may CREATE_EXAM_MANUAL");
        check(RequestAuthorizer.authorize(Command.CREATE_EXAM_VERSION, teacher1) == null,
                "teacher may CREATE_EXAM_VERSION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_EXAM_VERSION, student1),
                "student cannot CREATE_EXAM_VERSION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_EXAM_VERSION, coord1),
                "coordinator cannot CREATE_EXAM_VERSION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_EXAM_VERSION, principal1),
                "principal cannot CREATE_EXAM_VERSION");
        check(RequestAuthorizer.authorize(Command.CREATE_EXAM_EXECUTION, teacher1) == null,
                "teacher may CREATE_EXAM_EXECUTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_EXAM_EXECUTION, student1),
                "student cannot CREATE_EXAM_EXECUTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_EXAM_EXECUTION, principal1),
                "principal cannot CREATE_EXAM_EXECUTION");
        check(RequestAuthorizer.authorize(Command.GET_EXECUTION_STATS, teacher1) == null,
                "teacher may GET_EXECUTION_STATS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_EXECUTION_STATS, student1),
                "student cannot GET_EXECUTION_STATS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_EXECUTION_STATS, principal1),
                "principal cannot GET_EXECUTION_STATS");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_EXECUTION_STATS, coord1),
                "coordinator cannot GET_EXECUTION_STATS");
        check(RequestAuthorizer.authorize(Command.EXTEND_EXAM_TIME, teacher1) == null,
                "teacher may EXTEND_EXAM_TIME");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.EXTEND_EXAM_TIME, student1),
                "student cannot EXTEND_EXAM_TIME");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.EXTEND_EXAM_TIME, coord1),
                "coordinator cannot EXTEND_EXAM_TIME");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.EXTEND_EXAM_TIME, principal1),
                "principal cannot EXTEND_EXAM_TIME");

        check(RequestAuthorizer.authorize(Command.GET_PRINCIPAL_COMPARISON_REPORT, principal1) == null,
                "principal may GET_PRINCIPAL_COMPARISON_REPORT");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_PRINCIPAL_COMPARISON_REPORT, teacher1),
                "teacher cannot GET_PRINCIPAL_COMPARISON_REPORT");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_PRINCIPAL_COMPARISON_REPORT, student1),
                "student cannot GET_PRINCIPAL_COMPARISON_REPORT");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.GET_PRINCIPAL_COMPARISON_REPORT, coord1),
                "coordinator cannot GET_PRINCIPAL_COMPARISON_REPORT");

        check(student1.hasRole(AuthenticatedSession.STUDENT),
                "student1 has STUDENT role");
        check(coord1.hasRole(AuthenticatedSession.COORDINATOR),
                "SUBJECT_COORDINATOR matches COORDINATOR");
        check(!student2.hasRole(AuthenticatedSession.TEACHER),
                "student2 is not TEACHER");
        check(principal1.hasRole(AuthenticatedSession.PRINCIPAL),
                "principal role is recognized");

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
        check(expected.equals(actual), description + " (got: " + actual + ")");
    }
}
