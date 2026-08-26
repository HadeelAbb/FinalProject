package server.controllers;

import com.hsts.shared.net.Command;

import java.util.EnumMap;
import java.util.Map;

/**
 * Role gate for production commands. Uses AuthenticatedSession only —
 * never a DTO role or actor id.
 */
public final class RequestAuthorizer {

    public static final String AUTH_REQUIRED = "Authentication required.";
    public static final String NOT_AUTHORIZED = "You are not authorized to perform this action.";
    public static final String ONLY_COORDINATORS = "Only coordinators may approve or reject exams.";
    public static final String ONLY_TEACHERS_GRADE = "Only teachers may confirm grades.";
    public static final String OWN_RESULTS_ONLY = "Students may access only their own results.";

    private static final Map<Command, String[]> ROLES = new EnumMap<>(Command.class);

    static {
        ROLES.put(Command.GET_AVAILABLE_EXAMS, new String[]{AuthenticatedSession.STUDENT});
        ROLES.put(Command.START_EXAM, new String[]{AuthenticatedSession.STUDENT});
        ROLES.put(Command.SUBMIT_EXAM, new String[]{AuthenticatedSession.STUDENT});
        ROLES.put(Command.GET_MY_RESULTS, new String[]{AuthenticatedSession.STUDENT});
        ROLES.put(Command.GET_EXAM_ANSWER_COPY, new String[]{AuthenticatedSession.STUDENT});
        ROLES.put(Command.ASK_BOT_QUESTION, new String[]{AuthenticatedSession.STUDENT});
        ROLES.put(Command.GET_BOT_HISTORY, new String[]{AuthenticatedSession.STUDENT});

        ROLES.put(Command.SEARCH_QUESTIONS, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.CREATE_QUESTION, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.EDIT_QUESTION, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.DELETE_QUESTION, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.CREATE_EXAM_MANUAL, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.CREATE_EXAM_AUTO, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.CREATE_EXAM_VERSION, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.GET_MY_EXAMS, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.SUBMIT_EXAM_FOR_APPROVAL, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.GET_PENDING_GRADING, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.CONFIRM_GRADE, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.GET_EXAM_DETAIL, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.GET_EXAM_RESULTS, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.EXTEND_EXAM_TIME, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.CREATE_EXAM_EXECUTION, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.GET_EXAM_EXECUTIONS, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.GET_EXECUTION_STATS, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.GET_BOT_USAGE_STATS, new String[]{AuthenticatedSession.TEACHER});

        ROLES.put(Command.GET_PENDING_APPROVAL_EXAMS, new String[]{AuthenticatedSession.COORDINATOR});
        ROLES.put(Command.APPROVE_EXAM, new String[]{AuthenticatedSession.COORDINATOR});
        ROLES.put(Command.REJECT_EXAM, new String[]{AuthenticatedSession.COORDINATOR});

        ROLES.put(Command.GET_ALL_EXAMS, new String[]{AuthenticatedSession.PRINCIPAL});
        ROLES.put(Command.GET_ALL_RESULTS, new String[]{AuthenticatedSession.PRINCIPAL});
        ROLES.put(Command.GET_PRINCIPAL_COMPARISON_REPORT, new String[]{AuthenticatedSession.PRINCIPAL});
        ROLES.put(Command.GET_ALL_QUESTIONS, new String[]{AuthenticatedSession.PRINCIPAL});
        ROLES.put(Command.GET_EXAM_STATS, new String[]{
                AuthenticatedSession.TEACHER, AuthenticatedSession.PRINCIPAL});
        ROLES.put(Command.GET_BOT_CONFIG, new String[]{AuthenticatedSession.TEACHER});
        ROLES.put(Command.UPDATE_BOT_CONFIG, new String[]{AuthenticatedSession.TEACHER});
    }

    private RequestAuthorizer() {
    }

    /**
     * @return null if the command may proceed; otherwise a failure message.
     */
    public static String authorize(Command command, AuthenticatedSession session) {
        if (command == Command.LOGIN || command == Command.LOGOUT) {
            return null;
        }
        if (session == null || session.getUserId() == null || session.getUserId().isBlank()) {
            return AUTH_REQUIRED;
        }
        String[] allowed = ROLES.get(command);
        if (allowed == null) {
            return null;
        }
        if (session.hasRole(allowed)) {
            return null;
        }
        return failureMessage(command);
    }

    static String failureMessage(Command command) {
        if (command == Command.APPROVE_EXAM || command == Command.REJECT_EXAM
                || command == Command.GET_PENDING_APPROVAL_EXAMS) {
            return ONLY_COORDINATORS;
        }
        if (command == Command.CONFIRM_GRADE || command == Command.GET_PENDING_GRADING) {
            return ONLY_TEACHERS_GRADE;
        }
        if (command == Command.GET_MY_RESULTS || command == Command.GET_EXAM_ANSWER_COPY) {
            return OWN_RESULTS_ONLY;
        }
        return NOT_AUTHORIZED;
    }
}
