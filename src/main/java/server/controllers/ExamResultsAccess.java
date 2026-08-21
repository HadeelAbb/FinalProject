package server.controllers;

import com.hsts.shared.model.Exam;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.dto.CreateExamExecutionData;
import com.hsts.shared.net.dto.ExtendExamTimeData;
import com.hsts.shared.net.dto.GetExamDetailData;
import com.hsts.shared.net.dto.GetExamExecutionsData;
import com.hsts.shared.net.dto.GetExamStatsData;
import com.hsts.shared.net.dto.GetExecutionStatsData;

/**
 * Exam-level access using the authenticated session, not a DTO actor id.
 * Resource examId is not proof of authorization.
 */
public final class ExamResultsAccess {

    public static final String DENIED = "You are not authorized to view results for this exam.";
    public static final String NOT_FOUND = "Exam not found.";
    public static final String ACCESS_DENIED = "You are not authorized to access this exam.";

    private ExamResultsAccess() {
    }

    /**
     * @return null if the teacher may view this exam's results; otherwise a failure message.
     */
    public static String denyIfNotOwner(Exam exam, String teacherId) {
        if (exam == null) {
            return NOT_FOUND;
        }
        if (teacherId == null || teacherId.isBlank()
                || exam.getCreatedByTeacherId() == null
                || !teacherId.equals(exam.getCreatedByTeacherId())) {
            return DENIED;
        }
        return null;
    }

    public static boolean requiresExamAccessCheck(Command command) {
        return command == Command.GET_EXAM_STATS
                || command == Command.GET_EXAM_DETAIL
                || command == Command.GET_EXAM_EXECUTIONS
                || command == Command.CREATE_EXAM_EXECUTION
                || command == Command.GET_EXECUTION_STATS
                || command == Command.EXTEND_EXAM_TIME;
    }

    /** Commands whose resource id is an execution, not an exam. */
    public static boolean requiresExecutionLookup(Command command) {
        return command == Command.GET_EXECUTION_STATS
                || command == Command.EXTEND_EXAM_TIME;
    }

    public static String examIdFromPayload(Object payload) {
        if (payload instanceof GetExamDetailData data) {
            return data.getExamId();
        }
        if (payload instanceof GetExamExecutionsData data) {
            return data.getExamId();
        }
        if (payload instanceof GetExamStatsData data) {
            return data.getExamId();
        }
        if (payload instanceof CreateExamExecutionData data) {
            return data.getExamId();
        }
        return null;
    }

    public static String executionIdFromPayload(Object payload) {
        if (payload instanceof GetExecutionStatsData data) {
            return data.getExecutionId();
        }
        if (payload instanceof ExtendExamTimeData data) {
            return data.getExecutionId();
        }
        return null;
    }

    /**
     * Exam-level rule after the role gate.
     * Principal may request GET_EXAM_STATS for any exam. Teachers may access
     * exam/execution commands only for exams they created.
     *
     * @return null if allowed; otherwise a failure message.
     */
    public static String denyExamAccess(Command command, Exam exam, AuthenticatedSession session) {
        if (session == null || session.getUserId() == null || session.getUserId().isBlank()) {
            return RequestAuthorizer.AUTH_REQUIRED;
        }
        if (command == Command.GET_EXAM_STATS) {
            if (session.hasRole(AuthenticatedSession.PRINCIPAL)) {
                return null;
            }
            if (session.hasRole(AuthenticatedSession.TEACHER)) {
                return ownerOrDenied(exam, session.getUserId());
            }
            return RequestAuthorizer.NOT_AUTHORIZED;
        }
        if (command == Command.GET_EXAM_DETAIL
                || command == Command.GET_EXAM_EXECUTIONS
                || command == Command.CREATE_EXAM_EXECUTION
                || command == Command.GET_EXECUTION_STATS
                || command == Command.EXTEND_EXAM_TIME) {
            if (!session.hasRole(AuthenticatedSession.TEACHER)) {
                return RequestAuthorizer.NOT_AUTHORIZED;
            }
            return ownerOrDenied(exam, session.getUserId());
        }
        return null;
    }

    private static String ownerOrDenied(Exam exam, String teacherId) {
        String ownerError = denyIfNotOwner(exam, teacherId);
        if (ownerError == null) {
            return null;
        }
        if (NOT_FOUND.equals(ownerError)) {
            return NOT_FOUND;
        }
        return ACCESS_DENIED;
    }
}
