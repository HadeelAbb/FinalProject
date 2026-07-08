package com.hsts.client.network;

import com.hsts.shared.model.BotInteraction;
import com.hsts.shared.model.BotUsageStats;
import com.hsts.shared.model.Course;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.ExamStatus;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Student;
import com.hsts.shared.model.SubjectCoordinator;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.model.User;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.AskBotQuestionData;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.CreateExamAutoData;
import com.hsts.shared.net.dto.CreateExamManualData;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.DeleteQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;
import com.hsts.shared.net.dto.ExamApprovalDecisionData;
import com.hsts.shared.net.dto.ExtendExamTimeData;
import com.hsts.shared.net.dto.GetAvailableExamsData;
import com.hsts.shared.net.dto.GetBotHistoryData;
import com.hsts.shared.net.dto.GetBotUsageStatsData;
import com.hsts.shared.net.dto.GetExamDetailData;
import com.hsts.shared.net.dto.GetExamAnswerCopyData;
import com.hsts.shared.net.dto.GetMyExamsData;
import com.hsts.shared.net.dto.GetMyResultsData;
import com.hsts.shared.net.dto.GetPendingGradingData;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import com.hsts.shared.net.dto.StartExamData;
import com.hsts.shared.net.dto.SubmitExamData;
import com.hsts.shared.net.dto.SubmitExamForApprovalData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final Map<String, Student> studentsByUsername = new HashMap<>();
    private final Map<String, SubjectCoordinator> coordinatorsByUsername = new HashMap<>();
    private final Map<String, User> usersById = new HashMap<>();
    private final List<Question> questionBank = new ArrayList<>();
    private final List<Exam> exams = new ArrayList<>();
    private final List<ExamAnswer> examAnswers = new ArrayList<>();
    private final List<BotInteraction> botInteractions = new ArrayList<>();
    private int examSeq = 0;
    private int examAnswerSeq = 0;
    private int botInteractionSeq = 0;

    private final Course course11 = new Course("11", "Introduction to Computer Science");
    private final Course course22 = new Course("22", "Discrete Mathematics");

    public MockServerSimulator() {
        seedTeachers();
        seedStudentsAndCoordinators();
        seedQuestions();
    }

    private void seedStudentsAndCoordinators() {
        credentials.put("student1", "pass123");
        Student student1 = new Student("S1", "Noa", "Ben-David", "student1@school.edu", List.of(course11, course22));
        studentsByUsername.put("student1", student1);
        usersById.put(student1.getId(), student1);

        credentials.put("coordinator1", "pass123");
        SubjectCoordinator coord1 = new SubjectCoordinator("C1", "Rami", "Azoulay", "coordinator1@school.edu",
                List.of(course11, course22), "CS");
        coordinatorsByUsername.put("coordinator1", coord1);
        usersById.put(coord1.getId(), coord1);
    }

    private void seedTeachers() {
        credentials.put("teacher1", "pass123");
        Teacher t1 = new Teacher("T1", "Dana", "Levi", "teacher1@school.edu", List.of(course11, course22));
        teachersByUsername.put("teacher1", t1);
        usersById.put(t1.getId(), t1);

        credentials.put("teacher2", "pass123");
        Teacher t2 = new Teacher("T2", "Omer", "Cohen", "teacher2@school.edu", List.of(course11));
        teachersByUsername.put("teacher2", t2);
        usersById.put(t2.getId(), t2);
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

            case CREATE_EXAM_MANUAL -> handleCreateExamManual((CreateExamManualData) payload);
            case CREATE_EXAM_AUTO -> handleCreateExamAuto((CreateExamAutoData) payload);
            case GET_MY_EXAMS -> handleGetMyExams((GetMyExamsData) payload);
            case SUBMIT_EXAM_FOR_APPROVAL -> handleSubmitForApproval((SubmitExamForApprovalData) payload);
            case GET_PENDING_APPROVAL_EXAMS -> handleGetPendingApproval();
            case APPROVE_EXAM -> handleApprove((ExamApprovalDecisionData) payload);
            case REJECT_EXAM -> handleReject((ExamApprovalDecisionData) payload);

            case GET_AVAILABLE_EXAMS -> handleGetAvailableExams((GetAvailableExamsData) payload);
            case START_EXAM -> handleStartExam((StartExamData) payload);
            case SUBMIT_EXAM -> handleSubmitExam((SubmitExamData) payload);

            case GET_PENDING_GRADING -> handleGetPendingGrading((GetPendingGradingData) payload);
            case CONFIRM_GRADE -> handleConfirmGrade((ConfirmGradeData) payload);
            case GET_EXAM_DETAIL -> handleGetExamDetail((GetExamDetailData) payload);

            case GET_MY_RESULTS -> handleGetMyResults((GetMyResultsData) payload);
            case GET_EXAM_ANSWER_COPY -> handleGetExamAnswerCopy((GetExamAnswerCopyData) payload);

            case EXTEND_EXAM_TIME -> handleExtendTime((ExtendExamTimeData) payload);

            case ASK_BOT_QUESTION -> handleAskBot((AskBotQuestionData) payload);
            case GET_BOT_HISTORY -> handleGetBotHistory((GetBotHistoryData) payload);
            case GET_BOT_USAGE_STATS -> handleGetBotUsageStats((GetBotUsageStatsData) payload);

            default -> Response.failure(command, "Unsupported command", null);
        };
    }

    private Response handleLogin(LoginData data) {
        String storedPassword = credentials.get(data.getUsername());
        if (storedPassword == null || !storedPassword.equals(data.getPassword())) {
            return Response.failure(Command.LOGIN, "Invalid username or password.", null);
        }
        User user = coordinatorsByUsername.get(data.getUsername());
        if (user == null) {
            user = teachersByUsername.get(data.getUsername());
        }
        if (user == null) {
            user = studentsByUsername.get(data.getUsername());
        }
        return Response.success(Command.LOGIN, user, null, null);
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

    // ===================== SUC-2 / SUC-3: exam building =====================

    private Response handleCreateExamManual(CreateExamManualData data) {
        Teacher teacher = findTeacherById(data.getTeacherId());
        if (teacher == null || !teacher.teaches(data.getCourseId())) {
            return Response.failure(Command.CREATE_EXAM_MANUAL,
                    "You don't have permission to build exams for this course.", null);
        }
        if (data.getQuestionIds() == null || data.getQuestionIds().isEmpty()) {
            return Response.failure(Command.CREATE_EXAM_MANUAL, "An exam must contain at least one question.", null);
        }
        List<Question> chosen = data.getQuestionIds().stream()
                .map(this::findById)
                .filter(q -> q != null)
                .collect(Collectors.toList());
        if (chosen.size() != data.getQuestionIds().size()) {
            return Response.failure(Command.CREATE_EXAM_MANUAL, "One or more selected questions no longer exist.", null);
        }
        Exam exam = new Exam(nextExamId(), data.getCourseId(), data.getTitle(), data.getInstructionsForStudents(),
                chosen, data.getDurationMinutes(), data.getTeacherId());
        exams.add(exam);
        return Response.success(Command.CREATE_EXAM_MANUAL, exam, "Exam created as draft.", null);
    }

    private Response handleGetMyExams(GetMyExamsData data) {
        List<Exam> mine = exams.stream()
                .filter(e -> e.getCreatedByTeacherId().equals(data.getTeacherId()))
                .collect(Collectors.toList());
        return Response.success(Command.GET_MY_EXAMS, mine, null, null);
    }

    private Response handleCreateExamAuto(CreateExamAutoData data) {
        Teacher teacher = findTeacherById(data.getTeacherId());
        if (teacher == null || !teacher.teaches(data.getCourseId())) {
            return Response.failure(Command.CREATE_EXAM_AUTO,
                    "You don't have permission to build exams for this course.", null);
        }
        List<Question> matches = questionBank.stream()
                .filter(q -> q.getCourseId().equals(data.getCourseId()))
                .filter(q -> data.getTopic() == null || data.getTopic().isBlank()
                        || q.getTopic().toLowerCase().contains(data.getTopic().toLowerCase()))
                .filter(q -> data.getDifficulty() == null || q.getDifficulty() == data.getDifficulty())
                .limit(Math.max(data.getNumberOfQuestions(), 0))
                .collect(Collectors.toList());
        if (matches.size() < data.getNumberOfQuestions()) {
            return Response.failure(Command.CREATE_EXAM_AUTO,
                    "Not enough matching questions in the bank (found " + matches.size()
                            + ", requested " + data.getNumberOfQuestions() + ").", null);
        }
        Exam exam = new Exam(nextExamId(), data.getCourseId(), data.getTitle(), data.getInstructionsForStudents(),
                matches, data.getDurationMinutes(), data.getTeacherId());
        exams.add(exam);
        return Response.success(Command.CREATE_EXAM_AUTO, exam, "Exam auto-built as draft.", null);
    }

    // ===================== SUC-4: approval =====================

    private Response handleSubmitForApproval(SubmitExamForApprovalData data) {
        Exam exam = findExamById(data.getExamId());
        if (exam == null) {
            return Response.failure(Command.SUBMIT_EXAM_FOR_APPROVAL, "Exam not found.", null);
        }
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.REJECTED) {
            return Response.failure(Command.SUBMIT_EXAM_FOR_APPROVAL,
                    "Only a draft or rejected exam can be submitted for approval.", null);
        }
        exam.setStatus(ExamStatus.PENDING_APPROVAL);
        exam.setRejectionReason(null);
        return Response.success(Command.SUBMIT_EXAM_FOR_APPROVAL, exam, "Exam submitted for approval.", null);
    }

    private Response handleGetPendingApproval() {
        List<Exam> pending = exams.stream()
                .filter(e -> e.getStatus() == ExamStatus.PENDING_APPROVAL)
                .collect(Collectors.toList());
        return Response.success(Command.GET_PENDING_APPROVAL_EXAMS, pending, null, null);
    }

    private Response handleApprove(ExamApprovalDecisionData data) {
        Exam exam = findExamById(data.getExamId());
        if (exam == null) {
            return Response.failure(Command.APPROVE_EXAM, "Exam not found.", null);
        }
        exam.setStatus(ExamStatus.APPROVED);
        exam.setApprovedByCoordinatorId(data.getCoordinatorId());
        exam.setScheduledStart(LocalDateTime.now());
        exam.setScheduledEnd(LocalDateTime.now().plusDays(7));
        return Response.success(Command.APPROVE_EXAM, exam, "Exam approved.", null);
    }

    private Response handleReject(ExamApprovalDecisionData data) {
        Exam exam = findExamById(data.getExamId());
        if (exam == null) {
            return Response.failure(Command.REJECT_EXAM, "Exam not found.", null);
        }
        if (data.getReason() == null || data.getReason().isBlank()) {
            return Response.failure(Command.REJECT_EXAM, "A rejection reason is required.", null);
        }
        exam.setStatus(ExamStatus.REJECTED);
        exam.setRejectionReason(data.getReason());
        return Response.success(Command.REJECT_EXAM, exam, "Exam rejected.", null);
    }

    // ===================== SUC-6: taking an exam =====================

    private Response handleGetAvailableExams(GetAvailableExamsData data) {
        Student student = (Student) usersById.get(data.getStudentId());
        if (student == null) {
            return Response.failure(Command.GET_AVAILABLE_EXAMS, "Student not found.", null);
        }
        List<Exam> available = exams.stream()
                .filter(e -> e.getStatus() == ExamStatus.APPROVED)
                .filter(e -> student.enrolledIn(e.getCourseId()))
                .filter(e -> examAnswers.stream().noneMatch(
                        a -> a.getExamId().equals(e.getExamId()) && a.getStudentId().equals(student.getId())))
                .collect(Collectors.toList());
        return Response.success(Command.GET_AVAILABLE_EXAMS, available, null, null);
    }

    private Response handleStartExam(StartExamData data) {
        Exam exam = findExamById(data.getExamId());
        if (exam == null || exam.getStatus() != ExamStatus.APPROVED) {
            return Response.failure(Command.START_EXAM, "This exam is not available to take.", null);
        }
        boolean alreadyTaken = examAnswers.stream()
                .anyMatch(a -> a.getExamId().equals(data.getExamId()) && a.getStudentId().equals(data.getStudentId()));
        if (alreadyTaken) {
            return Response.failure(Command.START_EXAM, "You have already taken this exam.", null);
        }
        ExamAnswer answer = new ExamAnswer(nextExamAnswerId(), data.getExamId(), data.getStudentId());
        answer.setStartedAt(LocalDateTime.now());
        examAnswers.add(answer);
        // Client displays exam.getQuestions() without revealing which answer is correct.
        return Response.success(Command.START_EXAM, exam, "Exam started. Answer id: " + answer.getExamAnswerId(), null);
    }

    private Response handleSubmitExam(SubmitExamData data) {
        ExamAnswer answer = examAnswers.stream()
                .filter(a -> a.getExamId().equals(data.getExamId()) && a.getStudentId().equals(data.getStudentId()))
                .findFirst().orElse(null);
        if (answer == null) {
            return Response.failure(Command.SUBMIT_EXAM, "No in-progress attempt found for this exam.", null);
        }
        if (answer.getSubmittedAt() != null) {
            return Response.failure(Command.SUBMIT_EXAM, "This exam was already submitted.", null);
        }
        Exam exam = findExamById(data.getExamId());
        answer.setSelectedAnswers(data.getSelectedAnswers());
        answer.setSubmittedAt(LocalDateTime.now());
        answer.setAutoSubmitted(data.isAutoSubmitted());

        // ===== SUC-7: automatic grading, right at submission time =====
        double score = gradeExam(exam, answer);
        answer.setAutoScore(score);

        return Response.success(Command.SUBMIT_EXAM, answer, "Exam submitted and auto-graded.", null);
    }

    private double gradeExam(Exam exam, ExamAnswer answer) {
        if (exam == null || exam.getQuestions().isEmpty()) {
            return 0.0;
        }
        double points = exam.pointsPerQuestion();
        double total = 0.0;
        for (Question q : exam.getQuestions()) {
            String selected = answer.getSelectedAnswers().get(q.getQuestionId());
            QuestionAnswer correct = q.getCorrectAnswer();
            if (selected != null && correct != null && selected.equals(correct.getText())) {
                total += points;
            }
        }
        return Math.round(total * 100.0) / 100.0;
    }

    // ===================== SUC-7 / SUC-8: grading & confirmation =====================

    private Response handleGetExamDetail(GetExamDetailData data) {
        Exam exam = findExamById(data.getExamId());
        if (exam == null) {
            return Response.failure(Command.GET_EXAM_DETAIL, "Exam not found.", null);
        }
        return Response.success(Command.GET_EXAM_DETAIL, exam, null, null);
    }

    private Response handleGetPendingGrading(GetPendingGradingData data) {
        List<ExamAnswer> pending = examAnswers.stream()
                .filter(a -> a.getSubmittedAt() != null && !a.isGradeConfirmed())
                .filter(a -> {
                    Exam e = findExamById(a.getExamId());
                    return e != null && data.getTeacherId().equals(e.getCreatedByTeacherId());
                })
                .collect(Collectors.toList());
        return Response.success(Command.GET_PENDING_GRADING, pending, null, null);
    }

    private Response handleConfirmGrade(ConfirmGradeData data) {
        ExamAnswer answer = examAnswers.stream()
                .filter(a -> a.getExamAnswerId().equals(data.getExamAnswerId()))
                .findFirst().orElse(null);
        if (answer == null) {
            return Response.failure(Command.CONFIRM_GRADE, "Exam answer not found.", null);
        }
        boolean overridden = data.getFinalScore() != null
                && !data.getFinalScore().equals(answer.getAutoScore());
        if (overridden && (data.getTeacherComment() == null || data.getTeacherComment().isBlank())) {
            return Response.failure(Command.CONFIRM_GRADE,
                    "Changing the automatic score requires a comment explaining why.", null);
        }
        answer.setFinalScore(data.getFinalScore() != null ? data.getFinalScore() : answer.getAutoScore());
        answer.setTeacherComment(data.getTeacherComment());
        answer.setGradeConfirmed(true);
        return Response.success(Command.CONFIRM_GRADE, answer, "Grade confirmed.", null);
    }

    // ===================== SUC-10: viewing results =====================

    private Response handleGetMyResults(GetMyResultsData data) {
        List<ExamAnswer> mine = examAnswers.stream()
                .filter(a -> a.getStudentId().equals(data.getStudentId()) && a.isGradeConfirmed())
                .collect(Collectors.toList());
        return Response.success(Command.GET_MY_RESULTS, mine, null, null);
    }

    private Response handleGetExamAnswerCopy(GetExamAnswerCopyData data) {
        ExamAnswer answer = examAnswers.stream()
                .filter(a -> a.getExamAnswerId().equals(data.getExamAnswerId())
                        && a.getStudentId().equals(data.getStudentId()))
                .findFirst().orElse(null);
        if (answer == null || !answer.isGradeConfirmed()) {
            return Response.failure(Command.GET_EXAM_ANSWER_COPY, "Graded exam not found.", null);
        }
        Exam exam = findExamById(answer.getExamId());
        // Payload is a 2-element array: [Exam (with correct answers), ExamAnswer (student's own answers)].
        return Response.success(Command.GET_EXAM_ANSWER_COPY, new Object[]{exam, answer}, null, null);
    }

    // ===================== SUC-17: extend time mid-exam =====================

    private Response handleExtendTime(ExtendExamTimeData data) {
        Exam exam = findExamById(data.getExamId());
        if (exam == null) {
            return Response.failure(Command.EXTEND_EXAM_TIME, "Exam not found.", null);
        }
        if (!data.getTeacherId().equals(exam.getCreatedByTeacherId())) {
            return Response.failure(Command.EXTEND_EXAM_TIME, "Only the exam's creator can extend its time.", null);
        }
        exam.setDurationMinutes(exam.getDurationMinutes() + data.getAdditionalMinutes());
        return Response.success(Command.EXTEND_EXAM_TIME, exam,
                "Time extended by " + data.getAdditionalMinutes() + " minutes.", null);
    }

    // ===================== SUC-13/14/15: study bot =====================

    private Response handleAskBot(AskBotQuestionData data) {
        Student student = (Student) usersById.get(data.getStudentId());
        if (student == null || !student.enrolledIn(data.getCourseId())) {
            return Response.failure(Command.ASK_BOT_QUESTION,
                    "You must be enrolled in this course to use the bot.", null);
        }
        boolean examInProgress = examAnswers.stream()
                .filter(a -> a.getStudentId().equals(student.getId()) && a.getSubmittedAt() == null)
                .anyMatch(a -> {
                    Exam e = findExamById(a.getExamId());
                    return e != null && e.getCourseId().equals(data.getCourseId());
                });
        if (examInProgress) {
            return Response.failure(Command.ASK_BOT_QUESTION,
                    "The bot is unavailable while you have an exam in progress for this course.", null);
        }
        if (data.getQuestion() == null || data.getQuestion().isBlank()) {
            return Response.failure(Command.ASK_BOT_QUESTION, "Type a question first.", null);
        }

        // PARTNER 2/1 TODO: this is a canned placeholder, not a real AI call.
        // Swap this for an actual call to the external bot API (per spec
        // 3.1) - if the API returns nothing, show the same "no answer"
        // message this stub returns for blank questions above.
        String answer = "(simulated bot answer) I don't have a real AI connection yet, but here's an "
                + "acknowledgement of your question about \"" + data.getQuestion() + "\" - ask your teacher "
                + "for now, or check back once the real bot API is wired in.";

        BotInteraction interaction = new BotInteraction(nextBotInteractionId(), student.getId(),
                data.getCourseId(), data.getQuestion(), answer);
        botInteractions.add(interaction);
        return Response.success(Command.ASK_BOT_QUESTION, interaction, null, null);
    }

    private Response handleGetBotHistory(GetBotHistoryData data) {
        List<BotInteraction> mine = botInteractions.stream()
                .filter(i -> i.getStudentId().equals(data.getStudentId()))
                .collect(Collectors.toList());
        return Response.success(Command.GET_BOT_HISTORY, mine, null, null);
    }

    private Response handleGetBotUsageStats(GetBotUsageStatsData data) {
        Teacher teacher = findTeacherById(data.getTeacherId());
        if (teacher == null) {
            return Response.failure(Command.GET_BOT_USAGE_STATS, "Teacher not found.", null);
        }
        List<BotUsageStats> stats = new ArrayList<>();
        for (Course course : teacher.getCourses()) {
            List<BotInteraction> forCourse = botInteractions.stream()
                    .filter(i -> i.getCourseId().equals(course.getId()))
                    .collect(Collectors.toList());
            Set<String> uniqueStudents = forCourse.stream().map(BotInteraction::getStudentId)
                    .collect(Collectors.toCollection(HashSet::new));
            stats.add(new BotUsageStats(course.getId(), forCourse.size(), uniqueStudents.size()));
        }
        return Response.success(Command.GET_BOT_USAGE_STATS, stats, null, null);
    }

    // ===================== shared helpers =====================

    private Exam findExamById(String examId) {
        return exams.stream().filter(e -> e.getExamId().equals(examId)).findFirst().orElse(null);
    }

    private String nextExamId() {
        examSeq++;
        return "E" + examSeq;
    }

    private String nextExamAnswerId() {
        examAnswerSeq++;
        return "EA" + examAnswerSeq;
    }

    private String nextBotInteractionId() {
        botInteractionSeq++;
        return "BOT" + botInteractionSeq;
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
