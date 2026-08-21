package com.hsts.client.network;

import com.hsts.shared.model.*;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.*;

import java.util.List;
import java.util.Map;

public class SmokeTest {
    public static void main(String[] args) {
        MockServerSimulator sim = new MockServerSimulator();

        // 1. Teacher builds a manual exam on course 11
        Response loginTeacher = sim.process(Command.LOGIN, new LoginData("teacher1", "pass123"));
        check(loginTeacher.isSuccess(), "teacher login");
        Teacher teacher = (Teacher) loginTeacher.getPayload();

        Response search = sim.process(Command.SEARCH_QUESTIONS, new SearchQuestionsData("11", null, null));
        check(search.isSuccess(), "search questions");
        @SuppressWarnings("unchecked")
        List<Question> course11Questions = (List<Question>) search.getPayload();
        check(course11Questions.size() == 3, "course 11 has 3 seeded questions, got " + course11Questions.size());

        Response allQuestions = sim.process(Command.GET_ALL_QUESTIONS, new SearchQuestionsData(null, null, null));
        check(allQuestions.isSuccess(), "principal can retrieve all questions");
        @SuppressWarnings("unchecked")
        List<Question> bank = (List<Question>) allQuestions.getPayload();
        check(bank.stream().anyMatch(q -> "11".equals(q.getCourseId()))
                        && bank.stream().anyMatch(q -> "22".equals(q.getCourseId())),
                "principal all-question view includes multiple courses");
        Response csOnly = sim.process(Command.GET_ALL_QUESTIONS, new SearchQuestionsData("11", null, null));
        @SuppressWarnings("unchecked")
        List<Question> csOnlyList = (List<Question>) csOnly.getPayload();
        check(csOnly.isSuccess() && csOnlyList.stream().allMatch(q -> "11".equals(q.getCourseId())),
                "principal course filter returns only that course");
        Question sample = bank.get(0);
        String detail = com.hsts.shared.model.PrincipalQuestionDetailFormatter.format(sample);
        check(detail.contains("Correct answer:"), "principal detail includes the correct answer");
        check(sample.getAnswers() != null && sample.getAnswers().size() == 4,
                "principal sees four answers");

        List<String> qIds = course11Questions.stream().map(Question::getQuestionId).toList();
        Map<String, Integer> evenPoints = points100(qIds);
        Response createManual = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Midterm", "No calculators.",
                        null, qIds, evenPoints, 30));
        check(createManual.isSuccess(), "create manual exam: " + createManual.getMessage());
        Exam exam = (Exam) createManual.getPayload();
        check(exam.getStatus() == ExamStatus.DRAFT, "exam starts as DRAFT");
        check(exam.getVersionNumber() == 1 && exam.isLatest()
                        && exam.getExamId().equals(exam.getRootExamId()),
                "new exam is version 1 current");
        check(exam.getQuestions().size() == 3, "exam has 3 questions");

        // 2. Submit for approval
        Response submitApproval = sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(exam.getExamId(), teacher.getId()));
        check(submitApproval.isSuccess(), "submit for approval");
        check(((Exam) submitApproval.getPayload()).getStatus() == ExamStatus.PENDING_APPROVAL, "status is PENDING_APPROVAL");

        // 3. Coordinator approves
        Response loginCoord = sim.process(Command.LOGIN, new LoginData("coordinator1", "pass123"));
        check(loginCoord.isSuccess(), "coordinator login");
        SubjectCoordinator coord = (SubjectCoordinator) loginCoord.getPayload();

        Response pending = sim.process(Command.GET_PENDING_APPROVAL_EXAMS, null);
        check(pending.isSuccess(), "get pending approvals");
        @SuppressWarnings("unchecked")
        List<Exam> pendingList = (List<Exam>) pending.getPayload();
        check(pendingList.size() == 1, "1 exam pending approval, got " + pendingList.size());

        Response approve = sim.process(Command.APPROVE_EXAM,
                new ExamApprovalDecisionData(exam.getExamId(), coord.getId(), null));
        check(approve.isSuccess(), "approve exam: " + approve.getMessage());
        check(((Exam) approve.getPayload()).getStatus() == ExamStatus.APPROVED, "status is APPROVED");

        // 4. Student takes the exam
        Response loginStudent = sim.process(Command.LOGIN, new LoginData("student1", "pass123"));
        check(loginStudent.isSuccess(), "student login");
        Student student = (Student) loginStudent.getPayload();

        Response available = sim.process(Command.GET_AVAILABLE_EXAMS, new GetAvailableExamsData(student.getId()));
        check(available.isSuccess(), "get available exams");
        @SuppressWarnings("unchecked")
        List<Exam> availableList = (List<Exam>) available.getPayload();
        check(availableList.size() == 1, "1 exam available to student, got " + availableList.size());

        Response start = sim.process(Command.START_EXAM, new StartExamData(exam.getExamId(), student.getId()));
        check(start.isSuccess(), "start exam: " + start.getMessage());
        Exam startedExam = (Exam) start.getPayload();

        // Answer all 3 questions correctly using the actual correct answers
        Map<String, String> answers = new java.util.HashMap<>();
        for (Question q : startedExam.getQuestions()) {
            answers.put(q.getQuestionId(), q.getCorrectAnswer().getText());
        }
        Response submitExam = sim.process(Command.SUBMIT_EXAM,
                new SubmitExamData(exam.getExamId(), student.getId(), answers, false));
        check(submitExam.isSuccess(), "submit exam: " + submitExam.getMessage());
        ExamAnswer examAnswer = (ExamAnswer) submitExam.getPayload();
        check(examAnswer.getAutoScore() == 100.0, "auto score should be 100.0 for all-correct, got " + examAnswer.getAutoScore());

        // Duplicate attempt should be rejected
        Response secondStart = sim.process(Command.START_EXAM, new StartExamData(exam.getExamId(), student.getId()));
        check(!secondStart.isSuccess(), "second attempt at same exam should fail");

        // 5. Teacher grades
        Response pendingGrading = sim.process(Command.GET_PENDING_GRADING, new GetPendingGradingData(teacher.getId()));
        check(pendingGrading.isSuccess(), "get pending grading");
        @SuppressWarnings("unchecked")
        List<ExamAnswer> pendingGradingList = (List<ExamAnswer>) pendingGrading.getPayload();
        check(pendingGradingList.size() == 1, "1 submission pending grading, got " + pendingGradingList.size());

        // Confirm without changing score - should NOT require a comment
        Response confirm = sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswer.getExamAnswerId(), teacher.getId(), 100.0, null));
        check(confirm.isSuccess(), "confirm grade without override: " + confirm.getMessage());

        Response teacherResults = sim.process(Command.GET_EXAM_RESULTS,
                new GetExamResultsData(exam.getExamId(), teacher.getId()));
        check(teacherResults.isSuccess(), "teacher results for own exam");
        @SuppressWarnings("unchecked")
        List<ExamAnswer> teacherRows = (List<ExamAnswer>) teacherResults.getPayload();
        check(teacherRows.size() == 1, "1 result row for own exam, got " + teacherRows.size());
        check(teacherRows.get(0).getAutoScore() == 100.0,
                "teacher results keep the weighted auto score 100, got " + teacherRows.get(0).getAutoScore());
        int[] hist100 = com.hsts.shared.model.GradeHistogramCalculator.countsFromAnswers(teacherRows);
        check(hist100[9] == 1, "histogram places 100 in 90-100");

        Response loginTeacher2 = sim.process(Command.LOGIN, new LoginData("teacher2", "pass123"));
        check(loginTeacher2.isSuccess(), "teacher2 login");
        Teacher teacher2 = (Teacher) loginTeacher2.getPayload();
        Response forgedResults = sim.process(Command.GET_EXAM_RESULTS,
                new GetExamResultsData(exam.getExamId(), teacher2.getId()));
        check(!forgedResults.isSuccess(), "teacher2 cannot read teacher1 exam results");

        // 6. Student views results
        Response myResults = sim.process(Command.GET_MY_RESULTS, new GetMyResultsData(student.getId()));
        check(myResults.isSuccess(), "get my results");
        @SuppressWarnings("unchecked")
        List<ExamAnswer> myResultsList = (List<ExamAnswer>) myResults.getPayload();
        check(myResultsList.size() == 1, "1 confirmed result, got " + myResultsList.size());
        check(myResultsList.get(0).getFinalScore() == 100.0, "final score is 100.0");

        Response copy = sim.process(Command.GET_EXAM_ANSWER_COPY,
                new GetExamAnswerCopyData(examAnswer.getExamAnswerId(), student.getId()));
        check(copy.isSuccess(), "get exam answer copy");

        Question examV1 = exam.getQuestions().get(0);
        String originalQuestionId = examV1.getQuestionId();
        String originalQuestionText = examV1.getText();
        String originalCorrect = examV1.getCorrectAnswer().getText();

        List<QuestionAnswer> twoAnswers = List.of(
                new QuestionAnswer("only A", true),
                new QuestionAnswer("only B", false));
        Response invalidEdit = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData(originalQuestionId, "Updated", "", Difficulty.EASY, "Data Structures",
                        null, teacher.getId(), twoAnswers));
        check(!invalidEdit.isSuccess(), "invalid edit with 2 answers is denied");
        SearchQuestionsData afterInvalid = new SearchQuestionsData("11", null, null);
        @SuppressWarnings("unchecked")
        List<Question> stillThree = (List<Question>) sim.process(Command.SEARCH_QUESTIONS, afterInvalid).getPayload();
        check(stillThree.size() == 3, "failed edit does not persist a new version, got " + stillThree.size());
        check(stillThree.stream().anyMatch(q -> originalQuestionId.equals(q.getQuestionId())
                        && originalQuestionText.equals(q.getText()) && q.isLatest()),
                "v1 remains current after invalid edit");

        List<QuestionAnswer> v2Answers = List.of(
                new QuestionAnswer("A4", false),
                new QuestionAnswer("B5", false),
                new QuestionAnswer("C6", true),
                new QuestionAnswer("D7", false));
        Response editV2 = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData(originalQuestionId, "Updated", "new instructions", Difficulty.EASY,
                        "Data Structures", null, teacher.getId(), v2Answers));
        check(editV2.isSuccess(), "first edit creates v2: " + editV2.getMessage());
        Question v2 = (Question) editV2.getPayload();
        check(v2.getVersionNumber() == 2 && v2.isLatest(), "v2 is version 2 and current");
        check(!originalQuestionId.equals(v2.getQuestionId()), "v2 has a new physical question id");

        Response editHistorical = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData(originalQuestionId, "Should not overwrite v1", "", Difficulty.EASY,
                        "Data Structures", null, teacher.getId(), v2Answers));
        check(!editHistorical.isSuccess(), "historical v1 cannot be edited");

        List<QuestionAnswer> v3Answers = List.of(
                new QuestionAnswer("n1", false),
                new QuestionAnswer("n2", true),
                new QuestionAnswer("n3", false),
                new QuestionAnswer("n4", false));
        Response editV3 = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData(v2.getQuestionId(), "Updated again", "", Difficulty.MEDIUM,
                        "Data Structures", null, teacher.getId(), v3Answers));
        check(editV3.isSuccess(), "second edit creates v3");
        Question v3 = (Question) editV3.getPayload();
        check(v3.getVersionNumber() == 3 && v3.isLatest(), "v3 is the only current version");

        Response bankAll = sim.process(Command.SEARCH_QUESTIONS, new SearchQuestionsData("11", null, null));
        @SuppressWarnings("unchecked")
        List<Question> bankAllList = (List<Question>) bankAll.getPayload();
        check(bankAllList.size() == 5, "question bank shows v1+v2+v3 plus the other two questions, got "
                + bankAllList.size());
        check(bankAllList.stream().anyMatch(q -> originalQuestionId.equals(q.getQuestionId())
                        && originalQuestionText.equals(q.getText()) && !q.isLatest()),
                "v1 remains in the bank as historical with original text");
        check(bankAllList.stream().anyMatch(q -> q.getQuestionId().equals(v2.getQuestionId())
                        && "Updated".equals(q.getText()) && !q.isLatest()
                        && q.getCorrectAnswer() != null && "C6".equals(q.getCorrectAnswer().getText())),
                "v2 remains with its own answers/correct C");
        check(bankAllList.stream().filter(q -> originalQuestionId.equals(q.getRootQuestionId())
                        && q.isLatest()).count() == 1,
                "only one version in the lineage is current");

        SearchQuestionsData latestOnly = new SearchQuestionsData("11", null, null);
        latestOnly.setLatestOnly(true);
        Response latestSearch = sim.process(Command.SEARCH_QUESTIONS, latestOnly);
        @SuppressWarnings("unchecked")
        List<Question> latestList = (List<Question>) latestSearch.getPayload();
        check(latestList.size() == 3, "manual picker search returns latest only, got " + latestList.size());
        check(latestList.stream().noneMatch(q -> originalQuestionId.equals(q.getQuestionId())),
                "manual picker does not offer historical v1");
        check(latestList.stream().anyMatch(q -> v3.getQuestionId().equals(q.getQuestionId())),
                "manual picker offers v3");

        Response autoTooManyVersions = sim.process(Command.CREATE_EXAM_AUTO,
                new CreateExamAutoData(teacher.getId(), "11", "Too many", "", null, null, 4, 15));
        check(!autoTooManyVersions.isSuccess(),
                "automatic builder cannot treat v1/v2/v3 as three extra questions");

        Response detailAfterEdit = sim.process(Command.GET_EXAM_DETAIL, new GetExamDetailData(exam.getExamId()));
        Exam examAfterEdit = (Exam) detailAfterEdit.getPayload();
        check(examAfterEdit.getQuestions().stream().anyMatch(q -> originalQuestionId.equals(q.getQuestionId())
                        && originalQuestionText.equals(q.getText())),
                "existing exam still loads v1 after later versions exist");

        Response copyAfterEdit = sim.process(Command.GET_EXAM_ANSWER_COPY,
                new GetExamAnswerCopyData(examAnswer.getExamAnswerId(), student.getId()));
        Object[] copyPair = (Object[]) copyAfterEdit.getPayload();
        Exam copyExam = (Exam) copyPair[0];
        check(copyExam.getQuestions().stream().anyMatch(q -> originalQuestionId.equals(q.getQuestionId())
                        && originalCorrect.equals(q.getCorrectAnswer().getText())),
                "graded copy still uses v1 correct answer");

        Response resultsAfterEdit = sim.process(Command.GET_EXAM_RESULTS,
                new GetExamResultsData(exam.getExamId(), teacher.getId()));
        @SuppressWarnings("unchecked")
        List<ExamAnswer> resultsAfter = (List<ExamAnswer>) resultsAfterEdit.getPayload();
        check(resultsAfter.get(0).getAutoScore() == 100.0,
                "editing the bank does not change the exam's weighted auto score");

        Response principalHistory = sim.process(Command.GET_ALL_QUESTIONS, new SearchQuestionsData("11", null, null));
        @SuppressWarnings("unchecked")
        List<Question> principalRows = (List<Question>) principalHistory.getPayload();
        check(principalRows.stream().anyMatch(q -> originalQuestionId.equals(q.getQuestionId()) && !q.isLatest())
                        && principalRows.stream().anyMatch(q -> v3.getQuestionId().equals(q.getQuestionId()) && q.isLatest()),
                "principal read-only bank shows historical v1 and current v3");

        Response loginTeacher2Early = sim.process(Command.LOGIN, new LoginData("teacher2", "pass123"));
        Teacher teacher2early = (Teacher) loginTeacher2Early.getPayload();
        Response forgedCourseEdit = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData("00122", "Forged", "", Difficulty.HARD, "Set Theory",
                        null, teacher2early.getId(), v2Answers));
        check(!forgedCourseEdit.isSuccess(), "teacher2 cannot edit a course 22 question");
        Response forgedCourseDelete = sim.process(Command.DELETE_QUESTION,
                new DeleteQuestionData("00122", teacher2early.getId()));
        check(!forgedCourseDelete.isSuccess(), "teacher2 cannot delete a course 22 question");

        Response deleteUsedV1 = sim.process(Command.DELETE_QUESTION,
                new DeleteQuestionData(originalQuestionId, teacher.getId()));
        check(!deleteUsedV1.isSuccess(), "v1 used by Exam A cannot be deleted");
        check(server.controllers.QuestionVersioning.USED_BY_EXAM.equals(deleteUsedV1.getMessage()),
                "referenced delete explains that the version is used by an exam");
        Response detailAfterDeniedDelete = sim.process(Command.GET_EXAM_DETAIL, new GetExamDetailData(exam.getExamId()));
        Exam examAfterDeniedDelete = (Exam) detailAfterDeniedDelete.getPayload();
        check(examAfterDeniedDelete.getQuestions().stream().anyMatch(q -> originalQuestionId.equals(q.getQuestionId())
                        && originalQuestionText.equals(q.getText())
                        && originalCorrect.equals(q.getCorrectAnswer().getText())),
                "Exam A still contains v1 after denied delete");
        Response copyAfterDeniedDelete = sim.process(Command.GET_EXAM_ANSWER_COPY,
                new GetExamAnswerCopyData(examAnswer.getExamAnswerId(), student.getId()));
        Exam copyAfterDenied = (Exam) ((Object[]) copyAfterDeniedDelete.getPayload())[0];
        check(copyAfterDenied.getQuestions().stream().anyMatch(q -> originalQuestionId.equals(q.getQuestionId())
                        && originalCorrect.equals(q.getCorrectAnswer().getText())),
                "graded copy still loads v1 after denied delete");

        Response unusedCreate = sim.process(Command.CREATE_QUESTION,
                new CreateQuestionData("Unused lineage v1", "", Difficulty.EASY, "Data Structures",
                        null, "11", teacher.getId(), v2Answers));
        check(unusedCreate.isSuccess(), "create unused question v1");
        Question unusedV1 = (Question) unusedCreate.getPayload();
        Response unusedEdit = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData(unusedV1.getQuestionId(), "Unused lineage v2", "", Difficulty.EASY,
                        "Data Structures", null, teacher.getId(), v3Answers));
        check(unusedEdit.isSuccess(), "edit unused question to v2");
        Question unusedV2 = (Question) unusedEdit.getPayload();
        Response deleteUnusedHistorical = sim.process(Command.DELETE_QUESTION,
                new DeleteQuestionData(unusedV1.getQuestionId(), teacher.getId()));
        check(deleteUnusedHistorical.isSuccess(), "unused historical v1 can be deleted");
        Response unusedBank = sim.process(Command.SEARCH_QUESTIONS, new SearchQuestionsData("11", null, null));
        @SuppressWarnings("unchecked")
        List<Question> unusedBankList = (List<Question>) unusedBank.getPayload();
        check(unusedBankList.stream().noneMatch(q -> unusedV1.getQuestionId().equals(q.getQuestionId())),
                "deleted unused v1 is gone from the bank");
        check(unusedBankList.stream().anyMatch(q -> unusedV2.getQuestionId().equals(q.getQuestionId()) && q.isLatest()),
                "unused lineage v2 stays current after historical v1 delete");

        java.util.Map<String, Integer> v3OnlyPoints = java.util.Map.of(v3.getQuestionId(), 100);
        Response examBCreate = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Exam B latest", "",
                        null, List.of(v3.getQuestionId()), v3OnlyPoints, 20));
        check(examBCreate.isSuccess(), "Exam B uses current v3");
        Response deleteUsedCurrent = sim.process(Command.DELETE_QUESTION,
                new DeleteQuestionData(v3.getQuestionId(), teacher.getId()));
        check(!deleteUsedCurrent.isSuccess(), "current v3 used by Exam B cannot be deleted");
        Response afterDeniedCurrent = sim.process(Command.SEARCH_QUESTIONS, new SearchQuestionsData("11", null, null));
        @SuppressWarnings("unchecked")
        List<Question> afterDeniedCurrentList = (List<Question>) afterDeniedCurrent.getPayload();
        check(afterDeniedCurrentList.stream().anyMatch(q -> v3.getQuestionId().equals(q.getQuestionId()) && q.isLatest()),
                "v3 remains current after denied delete");
        Response examBDetail = sim.process(Command.GET_EXAM_DETAIL,
                new GetExamDetailData(((Exam) examBCreate.getPayload()).getExamId()));
        check(((Exam) examBDetail.getPayload()).getQuestions().stream()
                        .anyMatch(q -> v3.getQuestionId().equals(q.getQuestionId())),
                "Exam B still contains v3");

        // 7. Override-without-comment should fail (business rule check)
        // Build a second exam/answer pair quickly to test this in isolation.
        Response createManual2 = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Quiz 2", "",
                        null, qIds, evenPoints, 20));
        Exam exam2 = (Exam) createManual2.getPayload();
        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL, new SubmitExamForApprovalData(exam2.getExamId(), teacher.getId()));
        sim.process(Command.APPROVE_EXAM, new ExamApprovalDecisionData(exam2.getExamId(), coord.getId(), null));
        Response start2 = sim.process(Command.START_EXAM, new StartExamData(exam2.getExamId(), student.getId()));
        Exam startedExam2 = (Exam) start2.getPayload();
        Map<String, String> wrongAnswers = new java.util.HashMap<>();
        for (Question q : startedExam2.getQuestions()) {
            wrongAnswers.put(q.getQuestionId(), "definitely wrong");
        }
        Response submitExam2 = sim.process(Command.SUBMIT_EXAM,
                new SubmitExamData(exam2.getExamId(), student.getId(), wrongAnswers, false));
        ExamAnswer examAnswer2 = (ExamAnswer) submitExam2.getPayload();
        check(examAnswer2.getAutoScore() == 0.0, "auto score should be 0.0 for all-wrong, got " + examAnswer2.getAutoScore());
        Response exam2Results = sim.process(Command.GET_EXAM_RESULTS,
                new GetExamResultsData(exam2.getExamId(), teacher.getId()));
        @SuppressWarnings("unchecked")
        List<ExamAnswer> exam2Rows = (List<ExamAnswer>) exam2Results.getPayload();
        int[] hist0 = com.hsts.shared.model.GradeHistogramCalculator.countsFromAnswers(exam2Rows);
        check(hist0[0] == 1, "weighted zero grade lands in 0-9, not recalculated from question count");

        Response forgedConfirm = sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswer2.getExamAnswerId(), teacher2.getId(), 50.0, "Teacher2 override"));
        check(!forgedConfirm.isSuccess(), "teacher2 cannot confirm teacher1 submission");

        Response badOverride = sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswer2.getExamAnswerId(), teacher.getId(), 50.0, null));
        check(!badOverride.isSuccess(), "overriding score without a comment should fail");
        Response emptyOverride = sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswer2.getExamAnswerId(), teacher.getId(), 50.0, ""));
        check(!emptyOverride.isSuccess(), "overriding score with an empty comment should fail");
        Response whitespaceOverride = sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswer2.getExamAnswerId(), teacher.getId(), 50.0, "     "));
        check(!whitespaceOverride.isSuccess(), "overriding score with a whitespace comment should fail");

        Response goodOverride = sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswer2.getExamAnswerId(), teacher.getId(), 50.0, "Partial credit for effort."));
        check(goodOverride.isSuccess(), "overriding score with a comment should succeed: " + goodOverride.getMessage());
        ExamAnswer overridden = (ExamAnswer) goodOverride.getPayload();
        check(overridden.getAutoScore() == 0.0, "manual override must not overwrite autoScore, got " + overridden.getAutoScore());
        check(overridden.getFinalScore() == 50.0, "finalScore should be 50.0 after override, got " + overridden.getFinalScore());
        check("Partial credit for effort.".equals(overridden.getTeacherComment()),
                "grade-change reason persists on the confirmed answer");
        check(overridden.isGradeConfirmed(), "overridden grade is confirmed");
        Response myResultsAfterOverride = sim.process(Command.GET_MY_RESULTS, new GetMyResultsData(student.getId()));
        @SuppressWarnings("unchecked")
        List<ExamAnswer> afterOverride = (List<ExamAnswer>) myResultsAfterOverride.getPayload();
        ExamAnswer studentOverrideRow = afterOverride.stream()
                .filter(a -> examAnswer2.getExamAnswerId().equals(a.getExamAnswerId()))
                .findFirst().orElse(null);
        check(studentOverrideRow != null && studentOverrideRow.getFinalScore() == 50.0,
                "student results show the confirmed final score 50");
        check(studentOverrideRow != null && "Partial credit for effort.".equals(studentOverrideRow.getTeacherComment()),
                "student results still include the teacher comment/reason");

        // 8. Rejection path
        Response createManual3 = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Bad exam", "",
                        null, qIds, evenPoints, 15));
        Exam exam3 = (Exam) createManual3.getPayload();
        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL, new SubmitExamForApprovalData(exam3.getExamId(), teacher.getId()));
        Response reject = sim.process(Command.REJECT_EXAM,
                new ExamApprovalDecisionData(exam3.getExamId(), coord.getId(), "Too many questions from one topic."));
        check(reject.isSuccess(), "reject exam: " + reject.getMessage());
        check(((Exam) reject.getPayload()).getStatus() == ExamStatus.REJECTED, "status is REJECTED");
        Response rejectNoReason = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Bad exam 2", "",
                        null, qIds, evenPoints, 15));
        Exam exam4 = (Exam) rejectNoReason.getPayload();
        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL, new SubmitExamForApprovalData(exam4.getExamId(), teacher.getId()));
        Response rejectMissingReason = sim.process(Command.REJECT_EXAM,
                new ExamApprovalDecisionData(exam4.getExamId(), coord.getId(), ""));
        check(!rejectMissingReason.isSuccess(), "reject without reason should fail");

        // 9. Automatic exam building
        Response createAuto = sim.process(Command.CREATE_EXAM_AUTO,
                new CreateExamAutoData(teacher.getId(), "11", "Auto quiz", "", null, Difficulty.EASY, 1, 15));
        check(createAuto.isSuccess(), "auto-create exam: " + createAuto.getMessage());
        Exam autoExam = (Exam) createAuto.getPayload();
        check(autoExam.getQuestions().size() == 1, "auto exam has 1 question");
        check(autoExam.getQuestions().get(0).getDifficulty() == Difficulty.EASY, "auto exam question is EASY difficulty");
        check(autoExam.getQuestions().get(0).getPoints() == 100, "auto exam question has assigned 100 points");
        check(com.hsts.client.gui.ExamDraftReviewFormatter.sumAssignedPoints(autoExam) == 100,
                "auto exam review total uses assigned points");
        Response emptyResults = sim.process(Command.GET_EXAM_RESULTS,
                new GetExamResultsData(autoExam.getExamId(), teacher.getId()));
        check(emptyResults.isSuccess(), "results for an exam with no submissions succeed");
        @SuppressWarnings("unchecked")
        List<ExamAnswer> emptyRows = (List<ExamAnswer>) emptyResults.getPayload();
        check(emptyRows.isEmpty(), "exam with no submissions returns an empty result table");
        int[] emptyHist = com.hsts.shared.model.GradeHistogramCalculator.countsFromAnswers(emptyRows);
        check(java.util.Arrays.stream(emptyHist).allMatch(n -> n == 0),
                "empty results produce zero histogram counts");

        Response createAutoTooMany = sim.process(Command.CREATE_EXAM_AUTO,
                new CreateExamAutoData(teacher.getId(), "11", "Impossible quiz", "", null, Difficulty.HARD, 99, 15));
        check(!createAutoTooMany.isSuccess(), "auto-create with impossible count should fail");

        // 10. Extend time
        // Production DTO is (examId, executionId, teacherId, minutes). Mock still keys off examId;
        // executionId is unused by MockServerSimulator and left null here.
        Response extend = sim.process(Command.EXTEND_EXAM_TIME,
                new ExtendExamTimeData(autoExam.getExamId(), null, teacher.getId(), 10));
        check(extend.isSuccess(), "extend exam time: " + extend.getMessage());
        check(((Exam) extend.getPayload()).getDurationMinutes() == 25, "duration extended to 25");

        Response myExams = sim.process(Command.GET_MY_EXAMS, new GetMyExamsData(teacher.getId()));
        check(myExams.isSuccess(), "get my exams");
        @SuppressWarnings("unchecked")
        List<Exam> myExamsList = (List<Exam>) myExams.getPayload();
        check(myExamsList.size() >= 5, "teacher has at least 5 exams created, got " + myExamsList.size());

        // 11. Study bot - happy path
        Response ask = sim.process(Command.ASK_BOT_QUESTION,
                new AskBotQuestionData(student.getId(), "11", "What is a binary search tree?"));
        check(ask.isSuccess(), "ask bot: " + ask.getMessage());
        BotInteraction interaction = (BotInteraction) ask.getPayload();
        check(interaction.getAnswer() != null && !interaction.getAnswer().isBlank(), "bot answer is non-empty");

        Response askUnenrolled = sim.process(Command.ASK_BOT_QUESTION,
                new AskBotQuestionData(student.getId(), "99", "Question about a course I'm not in"));
        check(!askUnenrolled.isSuccess(), "asking bot about unenrolled course should fail");

        Response askBlank = sim.process(Command.ASK_BOT_QUESTION,
                new AskBotQuestionData(student.getId(), "11", ""));
        check(!askBlank.isSuccess(), "asking bot a blank question should fail");

        Response history = sim.process(Command.GET_BOT_HISTORY, new GetBotHistoryData(student.getId()));
        check(history.isSuccess(), "get bot history");
        @SuppressWarnings("unchecked")
        List<BotInteraction> historyList = (List<BotInteraction>) history.getPayload();
        check(historyList.size() == 1, "1 bot interaction in history, got " + historyList.size());

        // Bot should be blocked while student has an exam in progress for that course
        Response createManual5 = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Blocking exam", "",
                        null, qIds, evenPoints, 30));
        Exam exam5 = (Exam) createManual5.getPayload();
        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL, new SubmitExamForApprovalData(exam5.getExamId(), teacher.getId()));
        sim.process(Command.APPROVE_EXAM, new ExamApprovalDecisionData(exam5.getExamId(), coord.getId(), null));
        sim.process(Command.START_EXAM, new StartExamData(exam5.getExamId(), student.getId()));
        Response askDuringExam = sim.process(Command.ASK_BOT_QUESTION,
                new AskBotQuestionData(student.getId(), "11", "Can I ask during my exam?"));
        check(!askDuringExam.isSuccess(), "bot should be blocked while an exam is in progress for that course");
        // clean up the in-progress exam so it doesn't affect nothing else downstream
        sim.process(Command.SUBMIT_EXAM, new SubmitExamData(exam5.getExamId(), student.getId(), Map.of(), true));

        // 12. Teacher bot usage stats - anonymized
        Response stats = sim.process(Command.GET_BOT_USAGE_STATS, new GetBotUsageStatsData(teacher.getId()));
        check(stats.isSuccess(), "get bot usage stats");
        @SuppressWarnings("unchecked")
        List<BotUsageStats> statsList = (List<BotUsageStats>) stats.getPayload();
        BotUsageStats course11Stats = statsList.stream().filter(s -> s.getCourseId().equals("11")).findFirst().orElse(null);
        check(course11Stats != null && course11Stats.getTotalQuestions() == 1,
                "course 11 bot stats show 1 question, got " + (course11Stats != null ? course11Stats.getTotalQuestions() : "null"));
        check(course11Stats != null && course11Stats.getUniqueStudents() == 1,
                "course 11 bot stats show 1 unique student, got " + (course11Stats != null ? course11Stats.getUniqueStudents() : "null"));

        Response teacherCmp = sim.process(Command.GET_PRINCIPAL_COMPARISON_REPORT,
                new PrincipalComparisonReportData(PrincipalReportType.TEACHER, teacher.getId()));
        check(teacherCmp.isSuccess(), "principal teacher comparison");
        PrincipalComparisonReport teacherCmpReport = (PrincipalComparisonReport) teacherCmp.getPayload();
        check(teacherCmpReport.getRows().stream().allMatch(r -> teacher.getId().equals(r.getTeacherId())),
                "teacher comparison contains only that teacher's exams");
        PrincipalComparisonRow firstConfirmed = teacherCmpReport.getRows().stream()
                .filter(r -> exam.getExamId().equals(r.getExamId())).findFirst().orElse(null);
        check(firstConfirmed != null && firstConfirmed.getMean() == 100.0,
                "confirmed weighted 100 is used in principal teacher comparison");

        Response courseCmp = sim.process(Command.GET_PRINCIPAL_COMPARISON_REPORT,
                new PrincipalComparisonReportData(PrincipalReportType.COURSE, "11"));
        check(courseCmp.isSuccess(), "principal course comparison");

        Response studentCmp = sim.process(Command.GET_PRINCIPAL_COMPARISON_REPORT,
                new PrincipalComparisonReportData(PrincipalReportType.STUDENT, student.getId()));
        check(studentCmp.isSuccess(), "principal student comparison");
        PrincipalComparisonReport studentCmpReport = (PrincipalComparisonReport) studentCmp.getPayload();
        check(studentCmpReport.getRows().stream().anyMatch(r ->
                        exam.getExamId().equals(r.getExamId()) && r.getStudentGrade() != null && r.getStudentGrade() == 100.0),
                "student comparison shows student1 confirmed 100");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    static int failCount = 0;

    static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  OK  - " + description);
        } else {
            System.out.println("FAIL  - " + description);
            failCount++;
        }
    }

    private static Map<String, Integer> points100(List<String> ids) {
        Map<String, Integer> points = new java.util.LinkedHashMap<>();
        if (ids.size() == 3) {
            points.put(ids.get(0), 40);
            points.put(ids.get(1), 30);
            points.put(ids.get(2), 30);
            return points;
        }
        int each = ids.isEmpty() ? 0 : 100 / ids.size();
        for (String id : ids) {
            points.put(id, each);
        }
        return points;
    }
}
