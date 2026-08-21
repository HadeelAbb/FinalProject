package com.hsts.shared.model;

import com.hsts.client.network.MockServerSimulator;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.CreateExamManualData;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;
import com.hsts.shared.net.dto.ExamApprovalDecisionData;
import com.hsts.shared.net.dto.GetExamAnswerCopyData;
import com.hsts.shared.net.dto.GetExamDetailData;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import com.hsts.shared.net.dto.StartExamData;
import com.hsts.shared.net.dto.SubmitExamData;
import com.hsts.shared.net.dto.SubmitExamForApprovalData;
import server.controllers.AuthenticatedSession;
import server.controllers.RequestAuthorizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Illustration persistence through the mock OCSF path (no MySQL/JavaFX).
 */
public class QuestionIllustrationPersistenceTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        MockServerSimulator sim = new MockServerSimulator();
        Teacher teacher = (Teacher) sim.process(Command.LOGIN, new LoginData("teacher1", "pass123")).getPayload();
        SubjectCoordinator coord = (SubjectCoordinator) sim.process(
                Command.LOGIN, new LoginData("coordinator1", "pass123")).getPayload();
        Student student = (Student) sim.process(Command.LOGIN, new LoginData("student1", "pass123")).getPayload();

        List<QuestionAnswer> answers = List.of(
                new QuestionAnswer("A", true),
                new QuestionAnswer("B", false),
                new QuestionAnswer("C", false),
                new QuestionAnswer("D", false));

        CreateQuestionData noImage = new CreateQuestionData("No image question", "", Difficulty.EASY,
                "Illustrations", null, "11", teacher.getId(), answers);
        Response createdBare = sim.process(Command.CREATE_QUESTION, noImage);
        check(createdBare.isSuccess(), "1. create question without image");
        Question bare = (Question) createdBare.getPayload();
        check(!bare.hasIllustration(), "question without image remains valid");

        byte[] imageA = QuestionIllustrationTest.pngA();
        byte[] imageB = QuestionIllustrationTest.jpegB();
        CreateQuestionData withImage = new CreateQuestionData("Has image A", "", Difficulty.EASY,
                "Illustrations", "C:\\Users\\Teacher\\Desktop\\chart-a.png", "11", teacher.getId(), answers);
        withImage.setImageData(imageA);
        Response createdA = sim.process(Command.CREATE_QUESTION, withImage);
        check(createdA.isSuccess(), "2. create question with image");
        Question v1 = (Question) createdA.getPayload();
        check(v1.hasIllustration() && QuestionIllustration.sameBytes(imageA, v1.getImageData()),
                "created payload carries image A bytes");
        checkEquals("chart-a.png", v1.getImagePath(), "local path is not stored; filename only");

        @SuppressWarnings("unchecked")
        List<Question> searched = (List<Question>) sim.process(Command.SEARCH_QUESTIONS,
                new SearchQuestionsData("11", null, null)).getPayload();
        Question reloaded = searched.stream()
                .filter(q -> v1.getQuestionId().equals(q.getQuestionId()))
                .findFirst().orElse(null);
        check(reloaded != null && QuestionIllustration.sameBytes(imageA, reloaded.getImageData()),
                "2/3. SEARCH_QUESTIONS preserves image A after reload");

        Map<String, Integer> points = new HashMap<>();
        points.put(v1.getQuestionId(), 100);
        Response createExam = sim.process(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacher.getId(), "11", "Image exam", "",
                        null, List.of(v1.getQuestionId()), points, 20));
        check(createExam.isSuccess(), "exam created using physical v1");
        Exam exam = (Exam) createExam.getPayload();
        sim.process(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(exam.getExamId(), teacher.getId()));
        sim.process(Command.APPROVE_EXAM,
                new ExamApprovalDecisionData(exam.getExamId(), coord.getId(), null));

        @SuppressWarnings("unchecked")
        List<Question> all = (List<Question>) sim.process(Command.GET_ALL_QUESTIONS,
                new SearchQuestionsData(null, null, null)).getPayload();
        Question principalRow = all.stream()
                .filter(q -> v1.getQuestionId().equals(q.getQuestionId()))
                .findFirst().orElse(null);
        check(principalRow != null && QuestionIllustration.sameBytes(imageA, principalRow.getImageData()),
                "4. GET_ALL_QUESTIONS preserves image for Principal");
        String detail = PrincipalQuestionDetailFormatter.format(principalRow);
        check(detail.contains("Illustration: chart-a.png"), "principal detail mentions the illustration");

        EditQuestionData textOnly = new EditQuestionData(v1.getQuestionId(), "Has image A edited text", "",
                Difficulty.EASY, "Illustrations", null, teacher.getId(), answers);
        Response editText = sim.process(Command.EDIT_QUESTION, textOnly);
        check(editText.isSuccess(), "text-only edit creates v2");
        Question v2 = (Question) editText.getPayload();
        check(v2.getVersionNumber() == 2 && QuestionIllustration.sameBytes(imageA, v2.getImageData()),
                "5. v1 image A → edit text only → v2 keeps image A");

        @SuppressWarnings("unchecked")
        List<Question> afterText = (List<Question>) sim.process(Command.SEARCH_QUESTIONS,
                new SearchQuestionsData("11", null, null)).getPayload();
        Question v1AfterText = afterText.stream()
                .filter(q -> v1.getQuestionId().equals(q.getQuestionId()))
                .findFirst().orElse(null);
        check(v1AfterText != null && QuestionIllustration.sameBytes(imageA, v1AfterText.getImageData()),
                "v1 still has image A after text-only edit");

        Response start = sim.process(Command.START_EXAM, new StartExamData(exam.getExamId(), student.getId()));
        check(start.isSuccess(), "8. student START_EXAM succeeds");
        Exam started = (Exam) start.getPayload();
        Question examQuestion = started.getQuestions().stream()
                .filter(q -> v1.getQuestionId().equals(q.getQuestionId()))
                .findFirst().orElse(null);
        check(examQuestion != null && QuestionIllustration.sameBytes(imageA, examQuestion.getImageData()),
                "8. START_EXAM receives illustration A over the payload");

        Map<String, String> studentAnswers = new HashMap<>();
        studentAnswers.put(v1.getQuestionId(), "A");
        Response submit = sim.process(Command.SUBMIT_EXAM,
                new SubmitExamData(exam.getExamId(), student.getId(), studentAnswers, false));
        check(submit.isSuccess(), "student submit");
        ExamAnswer examAnswer = (ExamAnswer) submit.getPayload();
        Response confirm = sim.process(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswer.getExamAnswerId(), teacher.getId(), examAnswer.getAutoScore(), null));
        check(confirm.isSuccess(), "teacher confirms grade so the copy is available");

        EditQuestionData replace = new EditQuestionData(v2.getQuestionId(), "Has image B", "",
                Difficulty.EASY, "Illustrations", "chart-b.jpg", teacher.getId(), answers);
        replace.setImageData(imageB);
        Response editReplace = sim.process(Command.EDIT_QUESTION, replace);
        check(editReplace.isSuccess(), "replace image creates v3");
        Question v3 = (Question) editReplace.getPayload();
        check(QuestionIllustration.sameBytes(imageB, v3.getImageData()),
                "6. v2 replace → latest version has image B");

        @SuppressWarnings("unchecked")
        List<Question> afterReplace = (List<Question>) sim.process(Command.SEARCH_QUESTIONS,
                new SearchQuestionsData("11", null, null)).getPayload();
        Question v1AfterReplace = afterReplace.stream()
                .filter(q -> v1.getQuestionId().equals(q.getQuestionId()))
                .findFirst().orElse(null);
        Question v2AfterReplace = afterReplace.stream()
                .filter(q -> v2.getQuestionId().equals(q.getQuestionId()))
                .findFirst().orElse(null);
        check(v1AfterReplace != null && QuestionIllustration.sameBytes(imageA, v1AfterReplace.getImageData()),
                "6. v1 still retains image A");
        check(v2AfterReplace != null && QuestionIllustration.sameBytes(imageA, v2AfterReplace.getImageData()),
                "v2 still retains image A after later replace");

        Exam examAfter = (Exam) sim.process(Command.GET_EXAM_DETAIL, new GetExamDetailData(exam.getExamId()))
                .getPayload();
        check(examAfter.getQuestions().stream().anyMatch(q -> v1.getQuestionId().equals(q.getQuestionId())
                        && QuestionIllustration.sameBytes(imageA, q.getImageData())),
                "7/12. old exam using v1 still receives image A after v2/v3 exist");

        Response copyResp = sim.process(Command.GET_EXAM_ANSWER_COPY,
                new GetExamAnswerCopyData(examAnswer.getExamAnswerId(), student.getId()));
        check(copyResp.isSuccess(), "graded copy loads: " + copyResp.getMessage());
        Object[] copyPair = copyResp.getPayload() instanceof Object[] pair ? pair : null;
        Exam copyExam = copyPair != null ? (Exam) copyPair[0] : null;
        check(copyExam != null && copyExam.getQuestions().stream().anyMatch(q -> v1.getQuestionId().equals(q.getQuestionId())
                        && QuestionIllustration.sameBytes(imageA, q.getImageData())),
                "graded copy still uses image A from physical v1");

        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.CREATE_QUESTION,
                        new AuthenticatedSession("principal1", AuthenticatedSession.PRINCIPAL)),
                "9. Principal remains read-only for CREATE_QUESTION");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.EDIT_QUESTION,
                        new AuthenticatedSession("principal1", AuthenticatedSession.PRINCIPAL)),
                "9. Principal remains read-only for EDIT_QUESTION");
        check(RequestAuthorizer.authorize(Command.GET_ALL_QUESTIONS,
                new AuthenticatedSession("principal1", AuthenticatedSession.PRINCIPAL)) == null,
                "Principal may GET_ALL_QUESTIONS");

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
