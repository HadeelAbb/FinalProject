package server.controllers;

import com.hsts.shared.net.Command;
import com.hsts.shared.net.Request;
import com.hsts.shared.net.dto.AskBotQuestionData;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.CreateExamExecutionData;
import com.hsts.shared.net.dto.CreateExamVersionData;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.ExamApprovalDecisionData;
import com.hsts.shared.net.dto.ExtendExamTimeData;
import com.hsts.shared.net.dto.GetExamAnswerCopyData;
import com.hsts.shared.net.dto.GetExamDetailData;
import com.hsts.shared.net.dto.GetExamExecutionsData;
import com.hsts.shared.net.dto.GetExamResultsData;
import com.hsts.shared.net.dto.GetExamStatsData;
import com.hsts.shared.net.dto.GetExecutionStatsData;
import com.hsts.shared.net.dto.GetMyResultsData;
import com.hsts.shared.net.dto.PrincipalComparisonReportData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import com.hsts.shared.net.dto.StartExamData;
import com.hsts.shared.net.dto.SubmitExamData;

import java.util.List;
import java.util.Map;

/**
 * Actor-id overwrite checks. No MySQL, OCSF, or Groq.
 */
public class RequestIdentityBinderTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        AuthenticatedSession student1 = new AuthenticatedSession("student1", "STUDENT");
        AuthenticatedSession teacher1 = new AuthenticatedSession("teacher1", "TEACHER");
        AuthenticatedSession coord1 = new AuthenticatedSession("coord1", "SUBJECT_COORDINATOR");

        StartExamData start = new StartExamData("E61202", "student2", "ABCD");
        RequestIdentityBinder.bindActor(new Request(Command.START_EXAM, start, "r1"), student1);
        check("student1".equals(start.getStudentId()),
                "mismatched START_EXAM studentId is replaced with authenticated student1");
        check("E61202".equals(start.getExamId()),
                "START_EXAM examId resource id is preserved");

        SubmitExamData submit = new SubmitExamData("E61202", "student2", Map.of(), false);
        RequestIdentityBinder.bindActor(new Request(Command.SUBMIT_EXAM, submit, "r2"), student1);
        check("student1".equals(submit.getStudentId()),
                "mismatched SUBMIT_EXAM studentId is replaced with authenticated student1");

        GetMyResultsData results = new GetMyResultsData("student2");
        RequestIdentityBinder.bindActor(new Request(Command.GET_MY_RESULTS, results, "r3"), student1);
        check("student1".equals(results.getStudentId()),
                "GET_MY_RESULTS cannot request another student's rows");

        GetExamAnswerCopyData copy = new GetExamAnswerCopyData("EA1", "student2");
        RequestIdentityBinder.bindActor(new Request(Command.GET_EXAM_ANSWER_COPY, copy, "r4"), student1);
        check("student1".equals(copy.getStudentId()),
                "GET_EXAM_ANSWER_COPY studentId is forced to authenticated student");
        check("EA1".equals(copy.getExamAnswerId()),
                "GET_EXAM_ANSWER_COPY examAnswerId resource id is preserved");

        AskBotQuestionData bot = new AskBotQuestionData("student2", "CS101", "help?");
        RequestIdentityBinder.bindActor(new Request(Command.ASK_BOT_QUESTION, bot, "r5"), student1);
        check("student1".equals(bot.getStudentId()),
                "ASK_BOT_QUESTION uses authenticated student for enrollment and exam lock");
        check("CS101".equals(bot.getCourseId()),
                "ASK_BOT_QUESTION courseId resource id is preserved");

        ExamApprovalDecisionData approve = new ExamApprovalDecisionData("E1", "coord1", null);
        RequestIdentityBinder.bindActor(new Request(Command.APPROVE_EXAM, approve, "r6"), teacher1);
        check("teacher1".equals(approve.getCoordinatorId()),
                "forged coordinatorId is overwritten (role gate still rejects the teacher)");

        ExamApprovalDecisionData coordApprove = new ExamApprovalDecisionData("E1", "someoneElse", null);
        RequestIdentityBinder.bindActor(new Request(Command.APPROVE_EXAM, coordApprove, "r7"), coord1);
        check("coord1".equals(coordApprove.getCoordinatorId()),
                "legitimate coordinator actor id is the authenticated coordinator");

        ConfirmGradeData grade = new ConfirmGradeData("EA1", "teacher2", 80.0, "ok");
        RequestIdentityBinder.bindActor(new Request(Command.CONFIRM_GRADE, grade, "r8"), teacher1);
        check("teacher1".equals(grade.getTeacherId()),
                "CONFIRM_GRADE teacherId is the authenticated teacher");

        GetExamResultsData examResults = new GetExamResultsData("E-OTHER", "teacher2");
        RequestIdentityBinder.bindActor(new Request(Command.GET_EXAM_RESULTS, examResults, "r10"), teacher1);
        check("teacher1".equals(examResults.getTeacherId()),
                "GET_EXAM_RESULTS teacherId is overwritten with the authenticated teacher");
        check("E-OTHER".equals(examResults.getExamId()),
                "GET_EXAM_RESULTS examId resource id is preserved");

        GetExamDetailData detail = new GetExamDetailData("E-OTHER");
        RequestIdentityBinder.bindActor(new Request(Command.GET_EXAM_DETAIL, detail, "r11"), teacher1);
        check("E-OTHER".equals(detail.getExamId()),
                "GET_EXAM_DETAIL has no actor id to forge; examId is unchanged");

        GetExamExecutionsData executions = new GetExamExecutionsData("E-OTHER");
        RequestIdentityBinder.bindActor(new Request(Command.GET_EXAM_EXECUTIONS, executions, "r12"), teacher1);
        check("E-OTHER".equals(executions.getExamId()),
                "GET_EXAM_EXECUTIONS has no actor id to forge; examId is unchanged");

        GetExamStatsData stats = new GetExamStatsData("E-OTHER");
        RequestIdentityBinder.bindActor(new Request(Command.GET_EXAM_STATS, stats, "r13"), teacher1);
        check("E-OTHER".equals(stats.getExamId()),
                "GET_EXAM_STATS has no actor id to forge; examId is unchanged");

        CreateExamExecutionData createExec = new CreateExamExecutionData(
                "E-OTHER", "teacher2", "20-08-2026 09:00", "20-08-2026 11:00", "AB12");
        RequestIdentityBinder.bindActor(new Request(Command.CREATE_EXAM_EXECUTION, createExec, "r14"), teacher1);
        check("teacher1".equals(createExec.getTeacherId()),
                "CREATE_EXAM_EXECUTION teacherId is overwritten with the authenticated teacher");
        check("E-OTHER".equals(createExec.getExamId()),
                "CREATE_EXAM_EXECUTION examId resource id is preserved");

        GetExecutionStatsData execStats = new GetExecutionStatsData("EX-B");
        RequestIdentityBinder.bindActor(new Request(Command.GET_EXECUTION_STATS, execStats, "r15"), teacher1);
        check("EX-B".equals(execStats.getExecutionId()),
                "GET_EXECUTION_STATS has no actor id; executionId is unchanged");

        ExtendExamTimeData extend = new ExtendExamTimeData("E-FORGED", "EX-B", "teacher2", 10);
        RequestIdentityBinder.bindActor(new Request(Command.EXTEND_EXAM_TIME, extend, "r16"), teacher1);
        check("teacher1".equals(extend.getTeacherId()),
                "EXTEND_EXAM_TIME teacherId is overwritten with the authenticated teacher");
        check("EX-B".equals(extend.getExecutionId()),
                "EXTEND_EXAM_TIME executionId resource id is preserved");
        check("E-FORGED".equals(extend.getExamId()),
                "EXTEND_EXAM_TIME examId is not used for authorization");

        PrincipalComparisonReportData reportData = new PrincipalComparisonReportData(
                com.hsts.shared.model.PrincipalReportType.TEACHER, "teacher1");
        RequestIdentityBinder.bindActor(
                new Request(Command.GET_PRINCIPAL_COMPARISON_REPORT, reportData, "r17"), teacher1);
        check(com.hsts.shared.model.PrincipalReportType.TEACHER == reportData.getReportType()
                        && "teacher1".equals(reportData.getFilterValue()),
                "GET_PRINCIPAL_COMPARISON_REPORT has no actor id; filter value is unchanged");

        SearchQuestionsData allQuestions = new SearchQuestionsData("CS101", "Architecture", null);
        RequestIdentityBinder.bindActor(
                new Request(Command.GET_ALL_QUESTIONS, allQuestions, "r18"), teacher1);
        check("CS101".equals(allQuestions.getCourseId()) && "Architecture".equals(allQuestions.getTopic()),
                "GET_ALL_QUESTIONS has no actor id; course/topic filters are unchanged");

        CreateExamVersionData version = new CreateExamVersionData(
                "E-SOURCE", "otherTeacher", "Title", "students", "notes",
                List.of("Q1"), Map.of("Q1", 100), 60);
        RequestIdentityBinder.bindActor(new Request(Command.CREATE_EXAM_VERSION, version, "r19"), teacher1);
        check("teacher1".equals(version.getTeacherId()),
                "CREATE_EXAM_VERSION teacherId is overwritten with the authenticated teacher");
        check("E-SOURCE".equals(version.getSourceExamId()),
                "CREATE_EXAM_VERSION sourceExamId resource id is preserved");

        CreateQuestionData create = new CreateQuestionData();
        create.setTeacherId("otherTeacher");
        create.setCourseId("CS101");
        RequestIdentityBinder.bindActor(new Request(Command.CREATE_QUESTION, create, "r9"), teacher1);
        check("teacher1".equals(create.getTeacherId()),
                "CREATE_QUESTION teacherId is the authenticated teacher");
        check("CS101".equals(create.getCourseId()),
                "CREATE_QUESTION courseId resource id is preserved");

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
}
