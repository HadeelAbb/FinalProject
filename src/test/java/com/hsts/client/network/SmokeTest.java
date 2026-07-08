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

        List<String> qIds = course11Questions.stream().map(Question::getQuestionId).toList();
        Response createManual = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Midterm", "No calculators.", qIds, 30));
        check(createManual.isSuccess(), "create manual exam: " + createManual.getMessage());
        Exam exam = (Exam) createManual.getPayload();
        check(exam.getStatus() == ExamStatus.DRAFT, "exam starts as DRAFT");
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

        // 7. Override-without-comment should fail (business rule check)
        // Build a second exam/answer pair quickly to test this in isolation.
        Response createManual2 = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Quiz 2", "", qIds, 20));
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

        Response badOverride = sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswer2.getExamAnswerId(), teacher.getId(), 50.0, null));
        check(!badOverride.isSuccess(), "overriding score without a comment should fail");

        Response goodOverride = sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswer2.getExamAnswerId(), teacher.getId(), 50.0, "Partial credit for effort."));
        check(goodOverride.isSuccess(), "overriding score with a comment should succeed: " + goodOverride.getMessage());

        // 8. Rejection path
        Response createManual3 = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Bad exam", "", qIds, 15));
        Exam exam3 = (Exam) createManual3.getPayload();
        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL, new SubmitExamForApprovalData(exam3.getExamId(), teacher.getId()));
        Response reject = sim.process(Command.REJECT_EXAM,
                new ExamApprovalDecisionData(exam3.getExamId(), coord.getId(), "Too many questions from one topic."));
        check(reject.isSuccess(), "reject exam: " + reject.getMessage());
        check(((Exam) reject.getPayload()).getStatus() == ExamStatus.REJECTED, "status is REJECTED");
        Response rejectNoReason = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Bad exam 2", "", qIds, 15));
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

        Response createAutoTooMany = sim.process(Command.CREATE_EXAM_AUTO,
                new CreateExamAutoData(teacher.getId(), "11", "Impossible quiz", "", null, Difficulty.HARD, 99, 15));
        check(!createAutoTooMany.isSuccess(), "auto-create with impossible count should fail");

        // 10. Extend time
        Response extend = sim.process(Command.EXTEND_EXAM_TIME,
                new ExtendExamTimeData(autoExam.getExamId(), teacher.getId(), 10));
        check(extend.isSuccess(), "extend exam time: " + extend.getMessage());
        check(((Exam) extend.getPayload()).getDurationMinutes() == 25, "duration extended to 25");

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
}
