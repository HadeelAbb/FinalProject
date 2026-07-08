package com.hsts.client.network;

import com.hsts.shared.model.Course;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.DeleteQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.SearchQuestionsData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =====================================================================
 * TEMPORARY STAND-IN for Partner 1's DatabaseManager + LoginServerController
 * + QuestionServerController, all in one place, running in-process instead
 * of over a real connection.
 *
 * Seeded with the exact 6 questions from hsts_database_schema.sql so the
 * GUI behaves the same way it will once it's talking to the real DB.
 *
 * DELETE THIS CLASS once Partner 1 + Partner 2 deliver the real server -
 * it only exists so the GUI can be built and demoed independently.
 * =====================================================================
 */
public class MockServerSimulator {

    private final Map<String, String> credentials = new HashMap<>();
    private final Map<String, Teacher> teachersByUsername = new HashMap<>();
    private final List<Question> questionBank = new ArrayList<>();

    private final Course course11 = new Course("11", "Introduction to Computer Science");
    private final Course course22 = new Course("22", "Discrete Mathematics");

    public MockServerSimulator() {
        seedTeachers();
        seedQuestions();
    }

    private void seedTeachers() {
        credentials.put("teacher1", "pass123");
        teachersByUsername.put("teacher1",
                new Teacher("T1", "Dana", "Levi", "teacher1@school.edu", List.of(course11, course22)));

        credentials.put("teacher2", "pass123");
        teachersByUsername.put("teacher2",
                new Teacher("T2", "Omer", "Cohen", "teacher2@school.edu", List.of(course11)));
    }

    private void seedQuestions() {
        questionBank.add(new Question("00111",
                "What is the time complexity of searching in a perfectly balanced Binary Search Tree (BST)?",
                "Choose the single most accurate asymptotic upper bound.", Difficulty.MEDIUM,
                "Data Structures", null, "11", List.of(
                new QuestionAnswer("O(1)", false),
                new QuestionAnswer("O(log n)", true),
                new QuestionAnswer("O(n)", false),
                new QuestionAnswer("O(n log n)", false))));

        questionBank.add(new Question("00211",
                "Which of the following data structures operates strictly on a Last-In, First-Out (LIFO) basis?",
                "Select the correct foundational abstract data type.", Difficulty.EASY,
                "Data Structures", null, "11", List.of(
                new QuestionAnswer("Queue", false),
                new QuestionAnswer("Stack", true),
                new QuestionAnswer("Singly Linked List", false),
                new QuestionAnswer("Binary Tree", false))));

        questionBank.add(new Question("00311",
                "What occurs when a Java subclass defines a method with the exact same signature as a method in its superclass?",
                "Assume standard object-oriented programming behavior.", Difficulty.MEDIUM,
                "Object-Oriented Programming", null, "11", List.of(
                new QuestionAnswer("Method Overloading", false),
                new QuestionAnswer("Method Overriding", true),
                new QuestionAnswer("Compilation Error", false),
                new QuestionAnswer("Encapsulation Violation", false))));

        questionBank.add(new Question("00122",
                "Let A and B be finite sets. If |A| = 4 and |B| = 3, how many unique relations can be defined from set A to set B?",
                "Apply foundational set theory definitions.", Difficulty.HARD,
                "Set Theory", null, "22", List.of(
                new QuestionAnswer("12", false),
                new QuestionAnswer("64", false),
                new QuestionAnswer("4096", true),
                new QuestionAnswer("24", false))));

        questionBank.add(new Question("00222",
                "In graph theory, a tree is defined as an undirected graph that satisfies which of the following properties?",
                "Select the definitive structural criteria.", Difficulty.EASY,
                "Graph Theory", null, "22", List.of(
                new QuestionAnswer("Connected and contains no cycles", true),
                new QuestionAnswer("Disconnected and contains at least one cycle", false),
                new QuestionAnswer("Complete and directed", false),
                new QuestionAnswer("Bipartite and regular", false))));

        questionBank.add(new Question("00322",
                "Which of the following propositions is logically equivalent to the conditional statement p -> q?",
                "Apply standard logical equivalences.", Difficulty.MEDIUM,
                "Propositional Logic", null, "22", List.of(
                new QuestionAnswer("q -> p", false),
                new QuestionAnswer("not p or q", true),
                new QuestionAnswer("p and not q", false),
                new QuestionAnswer("not p and not q", false))));
    }

    public Response process(Command command, Object payload) {
        return switch (command) {
            case LOGIN -> handleLogin((LoginData) payload);
            case SEARCH_QUESTIONS -> handleSearch((SearchQuestionsData) payload);
            case CREATE_QUESTION -> handleCreate((CreateQuestionData) payload);
            case EDIT_QUESTION -> handleEdit((EditQuestionData) payload);
            case DELETE_QUESTION -> handleDelete((DeleteQuestionData) payload);
            default -> Response.failure(command, "Unsupported command", null);
        };
    }

    private Response handleLogin(LoginData data) {
        String storedPassword = credentials.get(data.getUsername());
        if (storedPassword == null || !storedPassword.equals(data.getPassword())) {
            return Response.failure(Command.LOGIN, "Invalid username or password.", null);
        }
        Teacher teacher = teachersByUsername.get(data.getUsername());
        return Response.success(Command.LOGIN, teacher, null, null);
    }

    private Response handleSearch(SearchQuestionsData criteria) {
        List<Question> results = new ArrayList<>();
        for (Question q : questionBank) {
            if (criteria.getCourseId() != null && !criteria.getCourseId().isBlank()
                    && !q.getCourseId().equals(criteria.getCourseId())) {
                continue;
            }
            if (criteria.getTopic() != null && !criteria.getTopic().isBlank()
                    && !q.getTopic().toLowerCase().contains(criteria.getTopic().toLowerCase())) {
                continue;
            }
            if (criteria.getDifficulty() != null && q.getDifficulty() != criteria.getDifficulty()) {
                continue;
            }
            results.add(q);
        }
        return Response.success(Command.SEARCH_QUESTIONS, results, null, null);
    }

    private Response handleCreate(CreateQuestionData data) {
        if (data.getAnswers() == null || data.getAnswers().size() != 4) {
            return Response.failure(Command.CREATE_QUESTION, "A question must have exactly 4 answers.", null);
        }
        long correctCount = data.getAnswers().stream().filter(QuestionAnswer::isCorrect).count();
        if (correctCount != 1) {
            return Response.failure(Command.CREATE_QUESTION, "Exactly one answer must be marked correct.", null);
        }

        Teacher teacher = findTeacherById(data.getTeacherId());
        if (teacher == null || !teacher.teaches(data.getCourseId())) {
            return Response.failure(Command.CREATE_QUESTION,
                    "You don't have permission to create questions for this course.", null);
        }

        String newId = generateQuestionId(data.getCourseId());
        Question question = new Question(newId, data.getText(), data.getInstructions(), data.getDifficulty(),
                data.getTopic(), data.getImagePath(), data.getCourseId(), data.getAnswers());
        questionBank.add(question);
        return Response.success(Command.CREATE_QUESTION, question, null, null);
    }

    private Teacher findTeacherById(String teacherId) {
        return teachersByUsername.values().stream()
                .filter(t -> t.getId().equals(teacherId))
                .findFirst().orElse(null);
    }

    private Response handleEdit(EditQuestionData data) {
        Question existing = findById(data.getQuestionId());
        if (existing == null) {
            return Response.failure(Command.EDIT_QUESTION, "Question " + data.getQuestionId() + " not found.", null);
        }
        if (data.getAnswers() != null) {
            if (data.getAnswers().size() != 4) {
                return Response.failure(Command.EDIT_QUESTION, "A question must have exactly 4 answers.", null);
            }
            long correctCount = data.getAnswers().stream().filter(QuestionAnswer::isCorrect).count();
            if (correctCount != 1) {
                return Response.failure(Command.EDIT_QUESTION, "Exactly one answer must be marked correct.", null);
            }
            existing.setAnswers(data.getAnswers());
        }
        existing.setText(data.getText());
        existing.setInstructions(data.getInstructions());
        existing.setDifficulty(data.getDifficulty());
        existing.setTopic(data.getTopic());
        existing.setImagePath(data.getImagePath());
        return Response.success(Command.EDIT_QUESTION, existing, null, null);
    }

    private Response handleDelete(DeleteQuestionData data) {
        Question existing = findById(data.getQuestionId());
        if (existing == null) {
            return Response.failure(Command.DELETE_QUESTION, "Question " + data.getQuestionId() + " not found.", null);
        }
        questionBank.remove(existing);
        return Response.success(Command.DELETE_QUESTION, data.getQuestionId(), null, null);
    }

    private Question findById(String questionId) {
        return questionBank.stream().filter(q -> q.getQuestionId().equals(questionId)).findFirst().orElse(null);
    }

    /**
     * Mirrors the ID scheme from the spec: 3-digit sequence number (per
     * course) + 2-digit course code, e.g. "00111", "00211" for course 11.
     */
    private String generateQuestionId(String courseId) {
        int maxSeq = 0;
        for (Question q : questionBank) {
            if (q.getCourseId().equals(courseId)) {
                String seqPart = q.getQuestionId().substring(0, 3);
                maxSeq = Math.max(maxSeq, Integer.parseInt(seqPart));
            }
        }
        int nextSeq = maxSeq + 1;
        return String.format("%03d%s", nextSeq, courseId);
    }
}
