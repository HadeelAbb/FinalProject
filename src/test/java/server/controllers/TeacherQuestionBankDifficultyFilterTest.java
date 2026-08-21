package server.controllers;

import com.hsts.client.network.MockServerSimulator;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.SearchQuestionsData;

import java.util.List;

/**
 * Teacher Question Bank SEARCH_QUESTIONS difficulty filter.
 * Does not change Exam Builder latest-only, Principal GET_ALL_QUESTIONS,
 * or automatic-exam selection.
 */
public class TeacherQuestionBankDifficultyFilterTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        MockServerSimulator sim = new MockServerSimulator();
        Teacher teacher1 = (Teacher) sim.process(Command.LOGIN, new LoginData("teacher1", "pass123")).getPayload();
        Teacher teacher2 = (Teacher) sim.process(Command.LOGIN, new LoginData("teacher2", "pass123")).getPayload();

        List<Question> any11 = search(sim, "11", null, null);
        check(any11.size() == 3, "1. Any/blank on course 11 returns all 3 questions, got " + any11.size());
        check(hasDifficulty(any11, Difficulty.EASY) && hasDifficulty(any11, Difficulty.MEDIUM),
                "1. Any includes EASY and MEDIUM");

        List<Question> easy11 = search(sim, "11", null, Difficulty.EASY);
        check(easy11.size() == 1 && allDifficulty(easy11, Difficulty.EASY),
                "2. EASY returns EASY only, got " + easy11.size());
        check(easy11.stream().anyMatch(q -> "00211".equals(q.getQuestionId())),
                "2. course 11 EASY is the seeded Stack question");

        List<Question> medium11 = search(sim, "11", null, Difficulty.MEDIUM);
        check(medium11.size() == 2 && allDifficulty(medium11, Difficulty.MEDIUM),
                "3. MEDIUM returns MEDIUM only, got " + medium11.size());
        check(!hasDifficulty(medium11, Difficulty.EASY) && !hasDifficulty(medium11, Difficulty.HARD),
                "3. MEDIUM excludes EASY and HARD");

        List<Question> hard22 = search(sim, "22", null, Difficulty.HARD);
        check(hard22.size() == 1 && allDifficulty(hard22, Difficulty.HARD),
                "4. HARD returns HARD only, got " + hard22.size());
        check(hard22.stream().anyMatch(q -> "00122".equals(q.getQuestionId())),
                "4. course 22 HARD is the seeded Set Theory question");

        List<Question> hard11 = search(sim, "11", null, Difficulty.HARD);
        check(hard11.isEmpty(), "4. course 11 has no HARD questions");

        List<Question> course22Medium = search(sim, "22", null, Difficulty.MEDIUM);
        check(course22Medium.size() == 1 && "00322".equals(course22Medium.get(0).getQuestionId()),
                "5. course + MEDIUM returns only that course's MEDIUM question");
        check(course22Medium.stream().noneMatch(q -> "11".equals(q.getCourseId())),
                "5. course 22 + MEDIUM excludes course 11 MEDIUM questions");

        List<Question> topicAndDiff = search(sim, "11", "Data Structures", Difficulty.MEDIUM);
        check(topicAndDiff.size() == 1 && "00111".equals(topicAndDiff.get(0).getQuestionId()),
                "6. topic + MEDIUM matches all supplied criteria, got " + topicAndDiff.size());
        check(topicAndDiff.stream().noneMatch(q -> "00211".equals(q.getQuestionId())),
                "6. Data Structures + MEDIUM excludes the EASY Data Structures question");

        List<QuestionAnswer> answers = List.of(
                new QuestionAnswer("A", true),
                new QuestionAnswer("B", false),
                new QuestionAnswer("C", false),
                new QuestionAnswer("D", false));
        Response forgedCreate = sim.process(Command.CREATE_QUESTION,
                new CreateQuestionData("Forged", "", Difficulty.EASY, "Set Theory",
                        null, "22", teacher2.getId(), answers));
        check(!forgedCreate.isSuccess(),
                "7. unauthorized Teacher course create remains denied");
        Response forgedEdit = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData("00122", "Forged", "", Difficulty.HARD, "Set Theory",
                        null, teacher2.getId(), answers));
        check(!forgedEdit.isSuccess(),
                "7. unauthorized Teacher course edit remains denied");
        checkEquals(RequestAuthorizer.NOT_AUTHORIZED,
                RequestAuthorizer.authorize(Command.SEARCH_QUESTIONS,
                        new AuthenticatedSession("student1", AuthenticatedSession.STUDENT)),
                "7. student cannot SEARCH_QUESTIONS");
        check(RequestAuthorizer.authorize(Command.SEARCH_QUESTIONS,
                new AuthenticatedSession("teacher1", AuthenticatedSession.TEACHER)) == null,
                "7. teacher may SEARCH_QUESTIONS");

        Question original = any11.stream().filter(q -> "00211".equals(q.getQuestionId())).findFirst().orElse(null);
        check(original != null && original.isLatest(), "8. seeded EASY question is current before edit");
        Response edit = sim.process(Command.EDIT_QUESTION,
                new EditQuestionData("00211", "Updated EASY text", "", Difficulty.EASY, "Data Structures",
                        null, teacher1.getId(), answers));
        check(edit.isSuccess(), "8. edit creates a new version: " + edit.getMessage());
        Question v2 = (Question) edit.getPayload();
        List<Question> afterEdit = search(sim, "11", null, Difficulty.EASY);
        check(afterEdit.stream().anyMatch(q -> "00211".equals(q.getQuestionId()) && !q.isLatest()
                        && "Which of the following data structures operates strictly on a Last-In, First-Out (LIFO) basis?"
                        .equals(q.getText())),
                "8. Question Bank still shows historical v1 after edit");
        check(afterEdit.stream().anyMatch(q -> v2.getQuestionId().equals(q.getQuestionId()) && q.isLatest()
                        && "Updated EASY text".equals(q.getText())),
                "8. Question Bank still shows current v2 after edit");
        SearchQuestionsData latestOnly = new SearchQuestionsData("11", null, null);
        latestOnly.setLatestOnly(true);
        @SuppressWarnings("unchecked")
        List<Question> picker = (List<Question>) sim.process(Command.SEARCH_QUESTIONS, latestOnly).getPayload();
        check(picker.stream().noneMatch(q -> "00211".equals(q.getQuestionId())),
                "8. Exam Builder latest-only still hides historical v1");
        check(picker.stream().anyMatch(q -> v2.getQuestionId().equals(q.getQuestionId())),
                "8. Exam Builder latest-only still offers current v2");
        check(QuestionVersioning.nextVersionNumber(1) == 2, "8. Question Versioning nextVersionNumber still 2");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Question> search(MockServerSimulator sim, String courseId, String topic,
                                         Difficulty difficulty) {
        Response response = sim.process(Command.SEARCH_QUESTIONS,
                new SearchQuestionsData(courseId, topic, difficulty));
        return (List<Question>) response.getPayload();
    }

    private static boolean hasDifficulty(List<Question> questions, Difficulty difficulty) {
        return questions.stream().anyMatch(q -> q.getDifficulty() == difficulty);
    }

    private static boolean allDifficulty(List<Question> questions, Difficulty difficulty) {
        return !questions.isEmpty() && questions.stream().allMatch(q -> q.getDifficulty() == difficulty);
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
