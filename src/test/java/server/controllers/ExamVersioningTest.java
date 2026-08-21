package server.controllers;

import com.hsts.client.network.MockServerSimulator;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.ExamExecution;
import com.hsts.shared.model.ExamStatus;
import com.hsts.shared.model.PrincipalComparisonReport;
import com.hsts.shared.model.PrincipalReportType;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Student;
import com.hsts.shared.model.SubjectCoordinator;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.CreateExamExecutionData;
import com.hsts.shared.net.dto.CreateExamManualData;
import com.hsts.shared.net.dto.CreateExamVersionData;
import com.hsts.shared.net.dto.EditQuestionData;
import com.hsts.shared.net.dto.ExamApprovalDecisionData;
import com.hsts.shared.net.dto.GetExamAnswerCopyData;
import com.hsts.shared.net.dto.GetExamDetailData;
import com.hsts.shared.net.dto.GetExamExecutionsData;
import com.hsts.shared.net.dto.GetExamResultsData;
import com.hsts.shared.net.dto.GetMyExamsData;
import com.hsts.shared.net.dto.GetMyResultsData;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.PrincipalComparisonReportData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import com.hsts.shared.net.dto.StartExamData;
import com.hsts.shared.net.dto.SubmitExamData;
import com.hsts.shared.net.dto.SubmitExamForApprovalData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exam edit/versioning checks. Uses the in-process mock (no MySQL/JavaFX).
 */
public class ExamVersioningTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        check(ExamVersioning.nextVersionNumber(1) == 2, "next after v1 is 2");
        check(ExamVersioning.nextVersionNumber(2) == 3, "next after v2 is 3");

        MockServerSimulator sim = new MockServerSimulator();
        Teacher teacher = (Teacher) sim.process(Command.LOGIN, new LoginData("teacher1", "pass123")).getPayload();
        Teacher teacher2 = (Teacher) sim.process(Command.LOGIN, new LoginData("teacher2", "pass123")).getPayload();
        SubjectCoordinator coord = (SubjectCoordinator) sim.process(
                Command.LOGIN, new LoginData("coordinator1", "pass123")).getPayload();
        Student student = (Student) sim.process(Command.LOGIN, new LoginData("student1", "pass123")).getPayload();

        SearchQuestionsData search = new SearchQuestionsData("11", null, null);
        search.setLatestOnly(true);
        @SuppressWarnings("unchecked")
        List<Question> bank = (List<Question>) sim.process(Command.SEARCH_QUESTIONS, search).getPayload();
        check(bank.size() == 3, "course 11 has 3 latest questions");
        List<String> qIds = bank.stream().map(Question::getQuestionId).toList();
        Map<String, Integer> even = points(qIds, 40, 30, 30);

        Exam v1 = createExam(sim, teacher, "Original", 60, qIds, even);
        check(v1.getVersionNumber() == 1 && v1.isLatest() && v1.getRootExamId().equals(v1.getExamId()),
                "new exam is version 1 current with root=self");
        check("Original".equals(v1.getTitle()) && v1.getDurationMinutes() == 60, "v1 stored original title/duration");

        Map<String, Integer> v1PointsSnapshot = pointsOf(v1);

        Response durationOnly = version(sim, teacher, v1, "Updated", 75, qIds, even);
        check(durationOnly.isSuccess(), "first edit preserves v1: " + durationOnly.getMessage());
        Exam v2 = (Exam) durationOnly.getPayload();
        Exam v1After = detail(sim, v1.getExamId());
        check("Original".equals(v1After.getTitle()) && v1After.getDurationMinutes() == 60,
                "v1 still exists: Original / 60");
        check("Updated".equals(v2.getTitle()) && v2.getDurationMinutes() == 75,
                "v2 exists: Updated / 75");
        check(!v1After.isLatest() && v1After.getVersionNumber() == 1, "v1 historical after first edit");
        check(v2.isLatest() && v2.getVersionNumber() == 2, "v2 current");
        check(v1After.getRootExamId().equals(v2.getRootExamId()), "v1 and v2 share lineage");

        Map<String, Integer> shifted = points(qIds, 10, 20, 70);
        Response pointsEdit = version(sim, teacher, v2, "Updated", 75, qIds, shifted);
        check(pointsEdit.isSuccess(), "questions/points independent: " + pointsEdit.getMessage());
        Exam v3 = (Exam) pointsEdit.getPayload();
        Exam v1Pts = detail(sim, v1.getExamId());
        Exam v2Pts = detail(sim, v2.getExamId());
        check(v1PointsSnapshot.equals(pointsOf(v1Pts)), "v1 points unchanged after later edits");
        check(points(qIds, 40, 30, 30).equals(pointsOf(v2Pts)), "v2 kept 40/30/30");
        check(shifted.equals(pointsOf(v3)), "v3 has 10/20/70");

        check(v1Pts.getVersionNumber() == 1 && v2Pts.getVersionNumber() == 2 && v3.getVersionNumber() == 3,
                "multiple edits keep versions 1/2/3");
        check(!v1Pts.isLatest() && !v2Pts.isLatest() && v3.isLatest(), "only v3 latest/current");

        Response historicalBranch = version(sim, teacher, v1After, "Branch", 80, qIds, even);
        check(!historicalBranch.isSuccess(), "historical branch denied");
        check(ExamVersioning.HISTORICAL_NOT_EDITABLE.equals(historicalBranch.getMessage()),
                "historical branch message is HISTORICAL_NOT_EDITABLE");
        check(detail(sim, v3.getExamId()).isLatest(), "failed branch leaves v3 current");

        Response ninety = version(sim, teacher, v3, "Bad", 75, qIds, points(qIds, 30, 30, 30));
        check(!ninety.isSuccess(), "90-point edit rejected");
        check(detail(sim, v3.getExamId()).isLatest(),
                "90-point reject leaves old version current");
        @SuppressWarnings("unchecked")
        List<Exam> afterNinety = (List<Exam>) sim.process(Command.GET_MY_EXAMS, new GetMyExamsData(teacher.getId()))
                .getPayload();
        long versions = afterNinety.stream().filter(e -> v1.getRootExamId().equals(e.getRootExamId())).count();
        check(versions == 3, "90-point reject created no extra version, got " + versions);

        Response zeroPts = version(sim, teacher, v3, "Bad", 75, qIds, points(qIds, 0, 50, 50));
        check(!zeroPts.isSuccess(), "zero/negative rejected");
        Response negPts = version(sim, teacher, v3, "Bad", 75, qIds, points(qIds, -10, 50, 60));
        check(!negPts.isSuccess(), "negative points rejected");

        Response otherTeacher = version(sim, teacher2, v3, "Stolen", 75, qIds, even);
        check(!otherTeacher.isSuccess(), "cross-teacher edit denied");
        check(RequestAuthorizer.NOT_AUTHORIZED.equals(otherTeacher.getMessage())
                        || otherTeacher.getMessage().contains("permission"),
                "cross-teacher uses authorization failure");

        Response ownCurrent = version(sim, teacher, v3, "Still mine", 80, qIds, even);
        check(ownCurrent.isSuccess(), "Teacher A edit own current exam");
        Exam v4 = (Exam) ownCurrent.getPayload();

        AuthenticatedSession principal = new AuthenticatedSession("principal1", "PRINCIPAL");
        AuthenticatedSession studentSess = new AuthenticatedSession("student1", "STUDENT");
        AuthenticatedSession coordSess = new AuthenticatedSession("coord1", "SUBJECT_COORDINATOR");
        AuthenticatedSession teacherSess = new AuthenticatedSession("teacher1", "TEACHER");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_EXAM_VERSION, principal),
                "Principal edit denied");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_EXAM_VERSION, studentSess),
                "Student edit denied");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_EXAM_VERSION, coordSess),
                "Coordinator edit denied");
        check(RequestAuthorizer.authorize(Command.CREATE_EXAM_VERSION, teacherSess) == null,
                "Teacher may CREATE_EXAM_VERSION");

        Exam rejectedV1 = createExam(sim, teacher, "Reject me", 60, qIds, even);
        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(rejectedV1.getExamId(), teacher.getId()));
        sim.process(Command.REJECT_EXAM,
                new ExamApprovalDecisionData(rejectedV1.getExamId(), coord.getId(), "Reason X"));
        Exam rejectedStored = detail(sim, rejectedV1.getExamId());
        check(rejectedStored.getStatus() == ExamStatus.REJECTED
                        && "Reason X".equals(rejectedStored.getRejectionReason()),
                "v1 REJECTED with reason X before edit");
        Response afterReject = version(sim, teacher, rejectedStored, "Fixed", 60, qIds, even);
        check(afterReject.isSuccess(), "edit of rejected current version");
        Exam rejectedAfter = detail(sim, rejectedV1.getExamId());
        Exam rejectedV2 = (Exam) afterReject.getPayload();
        check(rejectedAfter.getStatus() == ExamStatus.REJECTED
                        && "Reason X".equals(rejectedAfter.getRejectionReason()),
                "rejected version history preserved");
        check(rejectedV2.getStatus() == ExamStatus.DRAFT, "new version after reject is DRAFT");

        Exam execExam = createExam(sim, teacher, "Executable", 60, qIds, even);
        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(execExam.getExamId(), teacher.getId()));
        sim.process(Command.APPROVE_EXAM,
                new ExamApprovalDecisionData(execExam.getExamId(), coord.getId(), null));
        Exam approvedV1 = detail(sim, execExam.getExamId());
        check(approvedV1.getStatus() == ExamStatus.APPROVED, "v1 approved before student attempt");

        Response execV1 = sim.process(Command.CREATE_EXAM_EXECUTION, new CreateExamExecutionData(
                approvedV1.getExamId(), teacher.getId(), "01-01-2030 09:00", "01-01-2030 11:00", "ZX11"));
        check(execV1.isSuccess(), "execution on approved v1");
        ExamExecution executionX = (ExamExecution) execV1.getPayload();

        Response startV1 = sim.process(Command.START_EXAM,
                new StartExamData(approvedV1.getExamId(), student.getId(), approvedV1.getExecutionCode()));
        check(startV1.isSuccess(), "student starts v1: " + startV1.getMessage());
        Exam startedV1 = (Exam) startV1.getPayload();
        Map<String, String> allCorrect = new LinkedHashMap<>();
        for (Question q : startedV1.getQuestions()) {
            allCorrect.put(q.getQuestionId(), q.getCorrectAnswer().getText());
        }
        ExamAnswer submittedV1 = (ExamAnswer) sim.process(Command.SUBMIT_EXAM,
                new SubmitExamData(approvedV1.getExamId(), student.getId(), allCorrect, false)).getPayload();
        check(submittedV1.getAutoScore() == 100.0, "v1 weighted grading all-correct is 100");
        sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(submittedV1.getExamAnswerId(), teacher.getId(), 100.0, null));

        Response editApproved = version(sim, teacher, approvedV1, "Executable v2", 90, qIds, shifted);
        check(editApproved.isSuccess(), "edit approved exam creates v2");
        Exam approvedLineV2 = (Exam) editApproved.getPayload();
        Exam approvedLineV1 = detail(sim, approvedV1.getExamId());
        check(approvedLineV1.getStatus() == ExamStatus.APPROVED, "v1 APPROVED → v2 DRAFT: v1 remains APPROVED");
        check(approvedLineV2.getStatus() == ExamStatus.DRAFT, "v1 APPROVED → v2 DRAFT: v2 is DRAFT");

        Response execV2Draft = sim.process(Command.CREATE_EXAM_EXECUTION, new CreateExamExecutionData(
                approvedLineV2.getExamId(), teacher.getId(), "01-01-2030 12:00", "01-01-2030 14:00", "ZX22"));
        check(!execV2Draft.isSuccess(), "v2 requires reapproval");
        check("Only approved exams can be executed.".equals(execV2Draft.getMessage()),
                "draft v2 execution denied with approved-only message");

        @SuppressWarnings("unchecked")
        List<ExamExecution> stillV1 = (List<ExamExecution>) sim.process(
                Command.GET_EXAM_EXECUTIONS, new GetExamExecutionsData(approvedV1.getExamId())).getPayload();
        check(stillV1.stream().anyMatch(e -> executionX.getExecutionId().equals(e.getExecutionId())
                        && approvedV1.getExamId().equals(e.getExamId())),
                "old execution stays v1");

        @SuppressWarnings("unchecked")
        List<ExamAnswer> teacherV1Results = (List<ExamAnswer>) sim.process(
                Command.GET_EXAM_RESULTS, new GetExamResultsData(approvedV1.getExamId(), teacher.getId())).getPayload();
        check(teacherV1Results.stream().anyMatch(a -> submittedV1.getExamAnswerId().equals(a.getExamAnswerId())
                        && approvedV1.getExamId().equals(a.getExamId()) && a.getFinalScore() == 100.0),
                "old result stays v1");
        @SuppressWarnings("unchecked")
        List<ExamAnswer> myResults = (List<ExamAnswer>) sim.process(
                Command.GET_MY_RESULTS, new GetMyResultsData(student.getId())).getPayload();
        check(myResults.stream().anyMatch(a -> approvedV1.getExamId().equals(a.getExamId())),
                "student result remains associated with v1");

        Object[] copyPayload = (Object[]) sim.process(Command.GET_EXAM_ANSWER_COPY,
                new GetExamAnswerCopyData(submittedV1.getExamAnswerId(), student.getId())).getPayload();
        Exam copyExam = (Exam) copyPayload[0];
        check(approvedV1.getExamId().equals(copyExam.getExamId()), "graded copy stays v1");
        check(points(qIds, 40, 30, 30).equals(pointsOf(copyExam)), "graded copy still has v1 points");

        PrincipalComparisonReport report = (PrincipalComparisonReport) sim.process(
                Command.GET_PRINCIPAL_COMPARISON_REPORT,
                new PrincipalComparisonReportData(PrincipalReportType.TEACHER, teacher.getId())).getPayload();
        check(report.getRows().stream().anyMatch(r -> approvedV1.getExamId().equals(r.getExamId())),
                "principal stats stay on v1 physical exam");
        check(report.getRows().stream().noneMatch(r -> approvedLineV2.getExamId().equals(r.getExamId())
                        && r.getMean() != null && r.getConfirmedCount() > 0),
                "v2 with no confirmed results is not merged into v1 stats");

        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(approvedLineV2.getExamId(), teacher.getId()));
        sim.process(Command.APPROVE_EXAM,
                new ExamApprovalDecisionData(approvedLineV2.getExamId(), coord.getId(), null));
        Response execV2Ok = sim.process(Command.CREATE_EXAM_EXECUTION, new CreateExamExecutionData(
                approvedLineV2.getExamId(), teacher.getId(), "02-01-2030 09:00", "02-01-2030 11:00", "ZX23"));
        check(execV2Ok.isSuccess(), "after Coordinator approves v2, execution is allowed");

        Response startV2 = sim.process(Command.START_EXAM,
                new StartExamData(approvedLineV2.getExamId(), student.getId(),
                        detail(sim, approvedLineV2.getExamId()).getExecutionCode()));
        check(startV2.isSuccess(), "student can take v2 separately from v1");
        Exam startedV2 = (Exam) startV2.getPayload();
        Map<String, String> onlyFirst = new LinkedHashMap<>();
        Question first = startedV2.getQuestions().get(0);
        onlyFirst.put(first.getQuestionId(), first.getCorrectAnswer().getText());
        for (int i = 1; i < startedV2.getQuestions().size(); i++) {
            Question q = startedV2.getQuestions().get(i);
            String wrong = q.getAnswers().stream()
                    .filter(a -> !a.isCorrect())
                    .map(QuestionAnswer::getText)
                    .findFirst().orElse("");
            onlyFirst.put(q.getQuestionId(), wrong);
        }
        ExamAnswer v2Answer = (ExamAnswer) sim.process(Command.SUBMIT_EXAM,
                new SubmitExamData(approvedLineV2.getExamId(), student.getId(), onlyFirst, false)).getPayload();
        check(v2Answer.getAutoScore() == 10.0, "v2 weighted grading uses v2 points (10/20/70), got "
                + v2Answer.getAutoScore());
        check(detail(sim, approvedV1.getExamId()).getQuestions().get(0).getPoints() == 40,
                "weighted grading regression: v1 points remain 40");

        String carriedId = approvedLineV1.getQuestions().get(0).getQuestionId();
        List<QuestionAnswer> newAnswers = List.of(
                new QuestionAnswer("A4", false),
                new QuestionAnswer("B5", false),
                new QuestionAnswer("C6", true),
                new QuestionAnswer("D7", false));
        Response qEdit = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData(carriedId, "Updated bank text", "new instructions", Difficulty.EASY,
                        "Data Structures", null, teacher.getId(), newAnswers));
        check(qEdit.isSuccess(), "question version created after exams exist");
        Question qV2 = (Question) qEdit.getPayload();
        Exam afterQEditV1 = detail(sim, approvedLineV1.getExamId());
        Exam afterQEditV2 = detail(sim, approvedLineV2.getExamId());
        check(afterQEditV1.getQuestions().stream().anyMatch(q -> carriedId.equals(q.getQuestionId())),
                "question version regression: Exam v1 keeps old physical question id");
        check(afterQEditV2.getQuestions().stream().anyMatch(q -> carriedId.equals(q.getQuestionId())),
                "question version regression: Exam v2 keeps old physical question id");
        check(afterQEditV1.getQuestions().stream().noneMatch(q -> qV2.getQuestionId().equals(q.getQuestionId())),
                "exam v1 was not silently upgraded");

        Exam carrySource = createExam(sim, teacher, "Carry source", 60, qIds, even);
        String unchangedBankId = qIds.get(1);
        Response bankEdit = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData(unchangedBankId, "Bank v2", "", Difficulty.EASY, "Data Structures",
                        null, teacher.getId(), newAnswers));
        check(bankEdit.isSuccess(), "bank now has a newer question version: " + bankEdit.getMessage());
        Question newestQ = (Question) bankEdit.getPayload();
        Response durationCarry = version(sim, teacher, carrySource, "Carry source", 70, qIds, even);
        check(durationCarry.isSuccess(), "duration-only edit succeeds");
        Exam carriedExam = (Exam) durationCarry.getPayload();
        check(carriedExam.getQuestions().stream().anyMatch(q -> unchangedBankId.equals(q.getQuestionId())),
                "no silent question upgrade");
        check(carriedExam.getQuestions().stream().noneMatch(q -> newestQ.getQuestionId().equals(q.getQuestionId())),
                "duration-only edit did not switch to Q-v2");

        SearchQuestionsData latestPicker = new SearchQuestionsData("11", null, null);
        latestPicker.setLatestOnly(true);
        @SuppressWarnings("unchecked")
        List<Question> picker = (List<Question>) sim.process(Command.SEARCH_QUESTIONS, latestPicker).getPayload();
        check(picker.stream().noneMatch(q -> unchangedBankId.equals(q.getQuestionId()) && !q.isLatest()),
                "newly added question latest-only in SEARCH_QUESTIONS");
        String latestOfLineage0 = picker.stream()
                .filter(q -> qIds.get(0).equals(q.getRootQuestionId()) || qIds.get(0).equals(q.getQuestionId()))
                .map(Question::getQuestionId)
                .findFirst().orElse(qIds.get(0));
        List<String> addHistorical = new ArrayList<>(List.of(latestOfLineage0, qIds.get(2)));
        addHistorical.add(unchangedBankId);
        Map<String, Integer> addPts = new LinkedHashMap<>();
        addPts.put(latestOfLineage0, 40);
        addPts.put(qIds.get(2), 30);
        addPts.put(unchangedBankId, 30);
        Exam addSource = createExam(sim, teacher, "Add source", 60,
                List.of(latestOfLineage0, qIds.get(2)), Map.of(latestOfLineage0, 50, qIds.get(2), 50));
        Response addHist = version(sim, teacher, addSource, "Add source", 60, addHistorical, addPts);
        check(!addHist.isSuccess(), "newly added question latest-only denied on server");
        check(ExamVersioning.NEW_QUESTION_MUST_BE_CURRENT.equals(addHist.getMessage()),
                "historical bank version cannot be newly inserted");
        List<String> addLatest = new ArrayList<>(List.of(latestOfLineage0, qIds.get(2), newestQ.getQuestionId()));
        Map<String, Integer> addLatestPts = new LinkedHashMap<>();
        addLatestPts.put(latestOfLineage0, 40);
        addLatestPts.put(qIds.get(2), 30);
        addLatestPts.put(newestQ.getQuestionId(), 30);
        Response addOk = version(sim, teacher, addSource, "Add source", 60, addLatest, addLatestPts);
        check(addOk.isSuccess(), "newly added current question version is allowed");

        @SuppressWarnings("unchecked")
        List<Exam> mine = (List<Exam>) sim.process(Command.GET_MY_EXAMS, new GetMyExamsData(teacher.getId()))
                .getPayload();
        check(mine.stream().anyMatch(e -> e.getExamId().equals(v1.getExamId()) && !e.isLatest()),
                "teacher list shows historical v1");
        check(mine.stream().anyMatch(e -> e.getRootExamId().equals(v1.getRootExamId()) && e.isLatest()),
                "teacher list shows current version of the lineage");
        check(mine.stream().anyMatch(e ->
                        e.toString().contains("v1") && e.toString().contains("Historical")),
                "version list distinguishes Historical");
        check(mine.stream().anyMatch(e ->
                        e.toString().contains("Current") && e.toString().contains("DRAFT")
                                || e.toString().contains("Current")),
                "version list distinguishes Current");

        Exam resumeV1 = createExam(sim, teacher, "Resume source", 60, qIds, even);
        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(resumeV1.getExamId(), teacher.getId()));
        sim.process(Command.APPROVE_EXAM,
                new ExamApprovalDecisionData(resumeV1.getExamId(), coord.getId(), null));
        Response resumeEdit = version(sim, teacher, detail(sim, resumeV1.getExamId()),
                "Resume v2", 75, qIds, even);
        check(resumeEdit.isSuccess(), "Item 14 regression: approved v1 still creates v2 DRAFT");
        Exam resumeV2 = (Exam) resumeEdit.getPayload();
        check(resumeV2.getVersionNumber() == 2 && resumeV2.getStatus() == ExamStatus.DRAFT && resumeV2.isLatest(),
                "Item 14 regression: v2 is current DRAFT");
        check(detail(sim, resumeV1.getExamId()).getStatus() == ExamStatus.APPROVED
                        && !detail(sim, resumeV1.getExamId()).isLatest(),
                "Item 14 regression: v1 remains historical APPROVED");

        @SuppressWarnings("unchecked")
        List<Exam> reopened = (List<Exam>) sim.process(Command.GET_MY_EXAMS, new GetMyExamsData(teacher.getId()))
                .getPayload();
        Exam selectedV2 = reopened.stream()
                .filter(e -> resumeV2.getExamId().equals(e.getExamId()))
                .findFirst().orElse(null);
        check(selectedV2 != null && com.hsts.shared.model.ExamDraftActions.canSubmitForApproval(selectedV2),
                "reopened current DRAFT is submittable without creating a version");
        check(com.hsts.shared.model.ExamDraftActions.canReview(selectedV2),
                "reopened current DRAFT can be reviewed");
        check(selectedV2.getDurationMinutes() == 75 && even.equals(pointsOf(selectedV2)),
                "Review Draft uses v2 persisted duration/points");
        check("Total: 100 / 100".equals(com.hsts.client.gui.ExamDraftReviewFormatter.formatTotal(selectedV2)),
                "reopened draft review total is 100 / 100");
        long lineageBefore = reopened.stream()
                .filter(e -> resumeV1.getRootExamId().equals(e.getRootExamId()))
                .count();
        Response reopenSubmit = sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(selectedV2.getExamId(), teacher.getId()));
        check(reopenSubmit.isSuccess(), "reopened draft submit: " + reopenSubmit.getMessage());
        Exam submittedV2 = (Exam) reopenSubmit.getPayload();
        check(resumeV2.getExamId().equals(submittedV2.getExamId()) && submittedV2.getVersionNumber() == 2,
                "reopen v2 DRAFT submit keeps same examId and version 2");
        check(submittedV2.getStatus() == ExamStatus.PENDING_APPROVAL, "v2 moves to PENDING_APPROVAL");
        @SuppressWarnings("unchecked")
        List<Exam> afterSubmit = (List<Exam>) sim.process(Command.GET_MY_EXAMS, new GetMyExamsData(teacher.getId()))
                .getPayload();
        long lineageAfter = afterSubmit.stream()
                .filter(e -> resumeV1.getRootExamId().equals(e.getRootExamId()))
                .count();
        check(lineageBefore == lineageAfter, "submitting current DRAFT does not create v3");
        check(afterSubmit.stream().noneMatch(e ->
                        resumeV1.getRootExamId().equals(e.getRootExamId()) && e.getVersionNumber() == 3),
                "no accidental new version row");

        Response resubmitApproved = sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(resumeV1.getExamId(), teacher.getId()));
        check(!resubmitApproved.isSuccess(), "approved cannot resubmit");

        Exam histDraftV1 = createExam(sim, teacher, "Historical draft", 60, qIds, even);
        Exam histDraftV2 = (Exam) version(sim, teacher, histDraftV1, "Historical draft v2", 60, qIds, even)
                .getPayload();
        Response submitHistoricalDraft = sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(histDraftV1.getExamId(), teacher.getId()));
        check(!submitHistoricalDraft.isSuccess(), "historical draft cannot submit");
        check(server.controllers.ExamVersioning.HISTORICAL_NOT_SUBMITTABLE.equals(submitHistoricalDraft.getMessage()),
                "historical draft submit is denied as not current");
        check(!com.hsts.shared.model.ExamDraftActions.canSubmitForApproval(detail(sim, histDraftV1.getExamId())),
                "UI would disable Submit on historical DRAFT");
        Response submitCurrentHistLine = sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(histDraftV2.getExamId(), teacher.getId()));
        check(submitCurrentHistLine.isSuccess()
                        && histDraftV2.getExamId().equals(((Exam) submitCurrentHistLine.getPayload()).getExamId()),
                "only the current draft in the lineage can be submitted");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static Exam createExam(MockServerSimulator sim, Teacher teacher, String title, int duration,
                                   List<String> qIds, Map<String, Integer> points) {
        Response response = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", title, "Students", "Notes",
                        qIds, points, duration));
        check(response.isSuccess(), "create exam " + title + ": " + response.getMessage());
        return (Exam) response.getPayload();
    }

    private static Response version(MockServerSimulator sim, Teacher teacher, Exam source, String title,
                                    int duration, List<String> qIds, Map<String, Integer> points) {
        return sim.process(Command.CREATE_EXAM_VERSION,
                new CreateExamVersionData(source.getExamId(), teacher.getId(), title,
                        "Students", "Notes", qIds, points, duration));
    }

    private static Exam detail(MockServerSimulator sim, String examId) {
        return (Exam) sim.process(Command.GET_EXAM_DETAIL, new GetExamDetailData(examId)).getPayload();
    }

    private static Exam findExam(MockServerSimulator sim, Teacher teacher, String examId) {
        @SuppressWarnings("unchecked")
        List<Exam> mine = (List<Exam>) sim.process(Command.GET_MY_EXAMS, new GetMyExamsData(teacher.getId()))
                .getPayload();
        return mine.stream().filter(e -> examId.equals(e.getExamId())).findFirst().orElse(null);
    }

    private static Map<String, Integer> points(List<String> ids, int a, int b, int c) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(ids.get(0), a);
        map.put(ids.get(1), b);
        map.put(ids.get(2), c);
        return map;
    }

    private static Map<String, Integer> pointsOf(Exam exam) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Question q : exam.getQuestions()) {
            map.put(q.getQuestionId(), q.getPoints());
        }
        return map;
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
