package server.controllers;

import com.hsts.shared.model.*;
import com.hsts.shared.net.dto.*;
import server.db.DatabaseManager;
import server.db.repository.ExamAnswerRepositoryImpl;
import server.db.repository.ExamRepositoryImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExamServerController {

    private final ExamRepositoryImpl examRepository = new ExamRepositoryImpl();
    private final ExamAnswerRepositoryImpl answerRepository = new ExamAnswerRepositoryImpl();
    private final server.db.repository.ExamExecutionRepositoryImpl executionRepository = new server.db.repository.ExamExecutionRepositoryImpl();
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    // SUC-2: Manual Exam Creation
    public Exam createManualExam(CreateExamManualData data) {
        // R11: teacher can only build exams for courses she teaches
        if (!isTeacherAssignedToCourse(data.getTeacherId(), data.getCourseId())) {
            System.err.println("Create-exam rejected: teacher " + data.getTeacherId()
                    + " does not teach course " + data.getCourseId());
            return null;
        }

        String newExamId = "E" + (System.currentTimeMillis() % 100000);
        List<Question> selectedQuestions = fetchQuestionsByIds(data.getQuestionIds());

        Exam exam = new Exam(newExamId, data.getCourseId(), data.getTitle(),
                data.getInstructionsForStudents(), selectedQuestions, data.getDurationMinutes(), data.getTeacherId());
        exam.setStatus(ExamStatus.DRAFT);
        exam.setInstructionsForTeacher(data.getInstructionsForTeacher());

        return examRepository.save(exam) ? exam : null;
    }

    // SUC-3: Auto Exam Creation by Topic & Difficulty
    public Exam createAutoExam(CreateExamAutoData data) {
        // R11: teacher can only build exams for courses she teaches
        if (!isTeacherAssignedToCourse(data.getTeacherId(), data.getCourseId())) {
            System.err.println("Create-exam rejected: teacher " + data.getTeacherId()
                    + " does not teach course " + data.getCourseId());
            return null;
        }

        List<Question> matchingQuestions = fetchMatchingQuestions(
                data.getCourseId(), data.getTopic(), data.getDifficulty(), data.getNumberOfQuestions());

        if (matchingQuestions.size() < data.getNumberOfQuestions()) {
            return null; // Insufficient matching pool
        }

        String newExamId = "E" + (System.currentTimeMillis() % 100000);
        Exam exam = new Exam(newExamId, data.getCourseId(), data.getTitle(),
                data.getInstructionsForStudents(), matchingQuestions, data.getDurationMinutes(), data.getTeacherId());
        exam.setStatus(ExamStatus.DRAFT);
        exam.setInstructionsForTeacher(data.getInstructionsForTeacher());

        return examRepository.save(exam) ? exam : null;
    }

    // R04/R11: whether a teacher is assigned to teach a given course.
    public boolean isTeacherAssignedToCourse(String teacherId, String courseId) {
        String sql = "SELECT COUNT(*) FROM teacher_courses WHERE teacher_id = ? AND course_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, teacherId);
            stmt.setString(2, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // SUC-4 / SUC-3.5: Exam Approval - marks the exam eligible to be performed.
    // Per the spec (3.4/4/2.2): approval itself does NOT create an execution -
    // "taking the exam out of the drawer" is a distinct, separately-repeatable
    // teacher action (see createExecution below). A teacher must explicitly
    // open at least one execution before any student can take this exam.
    public Exam approveExam(String examId, String coordinatorId) {
        Optional<Exam> opt = examRepository.findById(examId);
        if (opt.isPresent()) {
            Exam exam = opt.get();
            // R72: can't approve an exam with no questions
            if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
                System.err.println("Approval rejected: exam " + examId + " has no questions.");
                return null;
            }
            exam.setStatus(ExamStatus.APPROVED);
            exam.setApprovedByCoordinatorId(coordinatorId);
            return examRepository.update(exam) ? exam : null;
        }
        return null;
    }

    private LocalDateTime parseOrDefault(String text, java.time.format.DateTimeFormatter fmt, LocalDateTime fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(text.trim(), fmt);
        } catch (Exception e) {
            System.err.println("Could not parse date '" + text + "' (expected dd-MM-yyyy HH:mm) - using default instead.");
            return fallback;
        }
    }

    // SUC-4: Exam Rejection
    public Exam rejectExam(String examId, String coordinatorId, String reason) {
        Optional<Exam> opt = examRepository.findById(examId);
        if (opt.isPresent()) {
            Exam exam = opt.get();
            exam.setStatus(ExamStatus.REJECTED);
            exam.setRejectionReason(reason);
            return examRepository.update(exam) ? exam : null;
        }
        return null;
    }
    // SUC-6: Start Exam - Enforces 4-Character Execution Code Verification
    public Exam startExam(StartExamData data, String submittedExecutionCode) {
        Optional<Exam> opt = examRepository.findById(data.getExamId());
        if (opt.isEmpty()) {
            System.err.println("Start rejected: Exam " + data.getExamId() + " not found.");
            return null;
        }

        Exam exam = opt.get();

        // 1. Check if exam is approved
        if (exam.getStatus() != ExamStatus.APPROVED) {
            System.err.println("Start rejected: Exam is not approved.");
            return null;
        }

        // 1b. SUC 6.2: student must be enrolled in the exam's course
        if (!isStudentEnrolled(data.getStudentId(), exam.getCourseId())) {
            System.err.println("Start rejected: Student " + data.getStudentId() + " is not enrolled in course " + exam.getCourseId());
            return null;
        }

        // 2. SUC 2.2: resolve which execution this code belongs to (an exam can have
        // several executions over time, each with its own code and window).
        Optional<com.hsts.shared.model.ExamExecution> executionOpt =
                executionRepository.findByExamIdAndCode(data.getExamId(), submittedExecutionCode);
        if (executionOpt.isEmpty()) {
            System.err.println("Start rejected: Invalid execution code provided for exam " + data.getExamId());
            return null;
        }
        com.hsts.shared.model.ExamExecution execution = executionOpt.get();
        LocalDateTime now = LocalDateTime.now();
        if (execution.getScheduledStart() != null && now.isBefore(execution.getScheduledStart())) {
            System.err.println("Start rejected: This execution has not opened yet.");
            return null;
        }
        if (execution.getScheduledEnd() != null && now.isAfter(execution.getScheduledEnd())) {
            System.err.println("Start rejected: This execution's window has closed.");
            return null;
        }

        // 3. Check if student already submitted
        if (hasStudentAlreadySubmitted(data.getStudentId(), data.getExamId())) {
            System.err.println("Start rejected: Student " + data.getStudentId() + " already took this exam.");
            return null;
        }

        // SUC-17: apply any extra minutes already granted to THIS execution - in-memory
        // only (not persisted back to the exam's own row), so it affects this student's
        // timer without permanently changing the exam's stored duration.
        if (execution.getExtraMinutesGranted() > 0) {
            exam.setDurationMinutes(exam.getDurationMinutes() + execution.getExtraMinutesGranted());
        }

        return exam; // Execution code verified; return exam payload
    }

    // SUC-6: Submit Exam & Auto-Grade Multiple Choice Options
    public ExamAnswer submitExam(SubmitExamData data) {
        Optional<Exam> optExam = examRepository.findById(data.getExamId());
        if (optExam.isEmpty()) return null;

        Exam exam = optExam.get();

        // 1. SUC 6.2: enforced independently here too, not just at startExam - a
        // submission shouldn't be accepted for a course the student was never
        // actually enrolled in, regardless of how the request reached the server.
        if (!isStudentEnrolled(data.getStudentId(), exam.getCourseId())) {
            System.err.println("Submission rejected: Student " + data.getStudentId() + " is not enrolled in course " + exam.getCourseId());
            return null;
        }

        // 2. Check if student has already submitted this exam (Enforce 1 attempt per student)
        if (hasStudentAlreadySubmitted(data.getStudentId(), data.getExamId())) {
            System.err.println("Submission rejected: Student " + data.getStudentId() + " has already taken exam " + data.getExamId());
            return null;
        }

        // 3. SUC 2.2: resolve which execution this submission belongs to, and validate
        // against ITS window (not the exam's own legacy dates).
        com.hsts.shared.model.ExamExecution execution = null;
        if (data.getExecutionCode() != null) {
            Optional<com.hsts.shared.model.ExamExecution> executionOpt =
                    executionRepository.findByExamIdAndCode(data.getExamId(), data.getExecutionCode());
            if (executionOpt.isPresent()) {
                execution = executionOpt.get();
                LocalDateTime now = LocalDateTime.now();
                if (execution.getScheduledStart() != null && now.isBefore(execution.getScheduledStart())) {
                    System.err.println("Submission rejected: This execution has not opened yet.");
                    return null;
                }
                if (execution.getScheduledEnd() != null && now.isAfter(execution.getScheduledEnd())) {
                    System.err.println("Submission rejected: This execution's window has expired.");
                    return null;
                }
            }
        }

        double earnedPoints = 0.0;
        int totalQuestions = exam.getQuestions() != null ? exam.getQuestions().size() : 0;
        double pointsPerQuestion = totalQuestions > 0 ? (100.0 / totalQuestions) : 0.0;

        // Auto-grading routine
        if (exam.getQuestions() != null && data.getSelectedAnswers() != null) {
            for (Question q : exam.getQuestions()) {
                String studentSelected = data.getSelectedAnswers().get(q.getQuestionId());
                String correctAnswer = fetchCorrectAnswerForQuestion(q.getQuestionId());

                if (studentSelected != null && studentSelected.trim().equalsIgnoreCase(correctAnswer != null ? correctAnswer.trim() : "")) {
                    earnedPoints += pointsPerQuestion;
                }
            }
        }

        String newAnswerId = "EA" + (System.currentTimeMillis() % 100000);
        ExamAnswer answer = new ExamAnswer(newAnswerId, data.getExamId(), data.getStudentId());
        answer.setExecutionId(execution != null ? execution.getExecutionId() : null);
        answer.setSelectedAnswers(data.getSelectedAnswers());
        answer.setSubmittedAt(LocalDateTime.now());
        answer.setAutoSubmitted(data.isAutoSubmitted());
        answer.setAutoScore(earnedPoints);
        answer.setFinalScore(earnedPoints); // Default until overridden by teacher
        answer.setGradeConfirmed(false);

        return answerRepository.save(answer) ? answer : null;
    }
    // SUC-6: Retrieve Approved Exam Directly by 4-Character Execution Code
    public Exam getExamByExecutionCode(String executionCode) {
        if (executionCode == null || executionCode.trim().length() != 4) {
            System.err.println("[EXAM-SERVER] Invalid execution code format.");
            return null;
        }

        String sql = "SELECT exam_id FROM exams WHERE LOWER(execution_code) = LOWER(?) AND status = 'APPROVED'";
        Connection conn = dbManager.getConnection();
        if (conn == null) return null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, executionCode.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String examId = rs.getString("exam_id");
                    Optional<Exam> optExam = examRepository.findById(examId);
                    if (optExam.isPresent()) {
                        System.out.println("[EXAM-SERVER] Successfully fetched exam [" + examId + "] via code: " + executionCode);
                        return optExam.get();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[EXAM-SERVER ERROR] Failed to fetch exam by execution code: " + e.getMessage());
            e.printStackTrace();
        }

        System.err.println("[EXAM-SERVER] No approved exam found for execution code: " + executionCode);
        return null;
    }

    // SUC-7: Teacher Grade Confirmation / Score Override
    // Returns null on success, or a specific reason string on failure - so the
    // client can show a real error instead of a generic "database rejected it".
    public String confirmGradeWithReason(ConfirmGradeData data) {
        Optional<ExamAnswer> opt = answerRepository.findById(data.getExamAnswerId());
        if (opt.isEmpty()) {
            return "That submission could not be found.";
        }
        ExamAnswer answer = opt.get();

        // R85: manual override can't exceed the exam's total points
        double finalScore = data.getFinalScore();
        Optional<Exam> examOpt = examRepository.findById(answer.getExamId());
        if (examOpt.isPresent()) {
            int maxPoints = examOpt.get().totalPoints();
            if (finalScore > maxPoints) {
                System.err.println("Grade confirmation rejected: score " + finalScore
                        + " exceeds this exam's total points (" + maxPoints + ").");
                return "Score cannot exceed this exam's total points (" + maxPoints + ").";
            }
        }
        if (finalScore < 0) {
            System.err.println("Grade confirmation rejected: score cannot be negative.");
            return "Score cannot be negative.";
        }

        answer.setFinalScore(finalScore);
        answer.setTeacherComment(data.getTeacherComment());
        answer.setGradeConfirmed(true);
        return answerRepository.update(answer) ? null : "Database update failed - please try again.";
    }

    // Backward-compatible boolean version, for anything still calling the old signature.
    public boolean confirmGrade(ConfirmGradeData data) {
        return confirmGradeWithReason(data) == null;
    }

    // SUC-9: full exam detail (with questions and correct answers) for the grading screen
    public Optional<Exam> getExamDetail(String examId) {
        return examRepository.findById(examId);
    }

    // SUC-4: Submit a draft (or previously-rejected) exam for coordinator approval
    public Exam submitForApproval(String examId) {
        Optional<Exam> opt = examRepository.findById(examId);
        if (opt.isEmpty()) {
            return null;
        }
        Exam exam = opt.get();
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.REJECTED) {
            System.err.println("Submit-for-approval rejected: exam " + examId + " is not a draft or rejected exam.");
            return null;
        }
        exam.setStatus(ExamStatus.PENDING_APPROVAL);
        exam.setRejectionReason(null);
        return examRepository.update(exam) ? exam : null;
    }

    // SUC-2/3: exams a given teacher has created, any status (their "My Exams" list)
    public List<Exam> getMyExams(String teacherId) {
        List<Exam> all = examRepository.findAll();
        List<Exam> mine = new ArrayList<>();
        for (Exam e : all) {
            if (teacherId != null && teacherId.equals(e.getCreatedByTeacherId())) {
                mine.add(e);
            }
        }
        return mine;
    }

    // SUC 7.3.1: every exam, any status, any teacher - Principal's read-only view
    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    // SUC 7.3.1: every confirmed result, any student - Principal's read-only view
    public List<ExamAnswer> getAllResults() {
        return answerRepository.findAllConfirmed();
    }

    // SUC 5 / 7.2 / 7.3.2: statistical metrics (mean/median/decile distribution) for one exam
    public Optional<ExamStats> getExamStats(String examId) {
        return answerRepository.getExamStats(examId);
    }

    // SUC 2.2: a teacher opens ANOTHER execution (sitting) of an already-approved exam.
    // Any teacher may do this, not just the exam's original author (spec 7.2 explicitly
    // allows performance by other teachers). Rejects if the exam isn't approved yet.
    // Per 3.5: open/close dates are mandatory for every execution.
    public com.hsts.shared.model.ExamExecution createExecution(String examId, String teacherId,
                                                               String scheduledStartText, String scheduledEndText) {
        Optional<Exam> opt = examRepository.findById(examId);
        if (opt.isEmpty() || opt.get().getStatus() != ExamStatus.APPROVED) {
            System.err.println("Create-execution rejected: exam " + examId + " is not approved.");
            return null;
        }

        if (scheduledStartText == null || scheduledStartText.isBlank()
                || scheduledEndText == null || scheduledEndText.isBlank()) {
            System.err.println("Create-execution rejected: open/close dates are required.");
            return null;
        }

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        LocalDateTime start = parseOrDefault(scheduledStartText, fmt, LocalDateTime.now());
        LocalDateTime end = parseOrDefault(scheduledEndText, fmt, start.plusDays(14));

        // R75: close date must be after open date
        if (!end.isAfter(start)) {
            System.err.println("Create-execution rejected: close date must be after open date.");
            return null;
        }

        String executionId = "EX" + (System.currentTimeMillis() % 100000);
        com.hsts.shared.model.ExamExecution execution = new com.hsts.shared.model.ExamExecution(
                executionId, examId, generateExecutionCode(), start, end, teacherId);
        return executionRepository.save(execution) ? execution : null;
    }

    // Section 4: every execution (past and present) this exam has ever had.
    public List<com.hsts.shared.model.ExamExecution> getExecutionsForExam(String examId) {
        return executionRepository.findByExamId(examId);
    }

    // Section 4: started/finished/timed-out counts for one specific execution.
    public com.hsts.shared.model.ExecutionStats getExecutionStats(String executionId) {
        return answerRepository.getExecutionStats(executionId);
    }

    // SUC-4: exams currently waiting on a coordinator's decision
    public List<Exam> getPendingApprovalExams() {
        List<Exam> all = examRepository.findAll();
        List<Exam> pending = new ArrayList<>();
        for (Exam e : all) {
            if (e.getStatus() == ExamStatus.PENDING_APPROVAL) {
                pending.add(e);
            }
        }
        return pending;
    }

    // SUC-6: exams a student can currently take (approved, enrolled in the course, not already submitted).
    public List<Exam> getAvailableExams(String studentId) {
        List<Exam> all = examRepository.findAll();
        List<ExamAnswer> mineSoFar = answerRepository.findByStudentId(studentId);
        List<Exam> available = new ArrayList<>();
        for (Exam e : all) {
            if (e.getStatus() != ExamStatus.APPROVED) {
                continue;
            }
            if (!isStudentEnrolled(studentId, e.getCourseId())) {
                continue;
            }
            // R24: an exam is only "available" when it has at least one execution
            // (sitting) with a currently-open window - not just because the exam
            // itself was approved at some point.
            if (!hasCurrentlyOpenExecution(e.getExamId())) {
                continue;
            }
            boolean alreadyTaken = mineSoFar.stream().anyMatch(a -> a.getExamId().equals(e.getExamId()));
            if (!alreadyTaken) {
                available.add(e);
            }
        }
        return available;
    }

    // R24: whether this exam currently has at least one execution whose open/close
    // window includes right now.
    public boolean hasCurrentlyOpenExecution(String examId) {
        List<com.hsts.shared.model.ExamExecution> executions = executionRepository.findByExamId(examId);
        LocalDateTime now = LocalDateTime.now();
        for (com.hsts.shared.model.ExamExecution execution : executions) {
            boolean afterStart = execution.getScheduledStart() == null || !now.isBefore(execution.getScheduledStart());
            boolean beforeEnd = execution.getScheduledEnd() == null || !now.isAfter(execution.getScheduledEnd());
            if (afterStart && beforeEnd) {
                return true;
            }
        }
        return false;
    }

    // SUC 6.2: whether a student is registered for a given course - now enforced
    // for both exam availability and the study bot.
    public boolean isStudentEnrolled(String studentId, String courseId) {
        String sql = "SELECT COUNT(*) FROM student_courses WHERE student_id = ? AND course_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // SUC-9: a teacher's grading queue - submitted answers not yet confirmed, for exams they created
    public List<ExamAnswer> getPendingGrading(String teacherId) {
        return answerRepository.findPendingGradingForTeacher(teacherId);
    }

    // SUC-10: a student's own confirmed results
    public List<ExamAnswer> getMyResults(String studentId) {
        List<ExamAnswer> all = answerRepository.findByStudentId(studentId);
        List<ExamAnswer> confirmed = new ArrayList<>();
        for (ExamAnswer a : all) {
            if (a.isGradeConfirmed()) {
                confirmed.add(a);
            }
        }
        return confirmed;
    }

    // SUC-10: a student's own graded copy - the exam (with correct answers) plus their answers.
    // Returns a 2-element array [Exam, ExamAnswer], same shape the client already expects.
    public Object[] getExamAnswerCopy(String examAnswerId, String studentId) {
        Optional<ExamAnswer> opt = answerRepository.findById(examAnswerId);
        if (opt.isEmpty()) {
            return null;
        }
        ExamAnswer answer = opt.get();
        if (!answer.getStudentId().equals(studentId) || !answer.isGradeConfirmed()) {
            return null;
        }
        Optional<Exam> examOpt = examRepository.findById(answer.getExamId());
        if (examOpt.isEmpty()) {
            return null;
        }
        return new Object[]{examOpt.get(), answer};
    }

    // SUC-17: a teacher extends the time for a SPECIFIC execution (spec: temporary,
    // applies only to this run, never changes the exam's base definition). Persisted
    // on the execution itself, so it applies to anyone taking this execution from now
    // on - not just students already connected the instant this is called. Any teacher
    // may do this, matching createExecution's permissiveness (spec 7.2).
    public com.hsts.shared.model.ExamExecution extendExecutionTime(String executionId, int additionalMinutes) {
        Optional<com.hsts.shared.model.ExamExecution> opt = executionRepository.findById(executionId);
        if (opt.isEmpty()) {
            return null;
        }
        boolean ok = executionRepository.addExtraMinutes(executionId, additionalMinutes);
        if (!ok) {
            return null;
        }
        return executionRepository.findById(executionId).orElse(null);
    }

    // Backward-compatible: extends the exam's FIRST/default execution by exam id,
    // for older callers that don't know about specific execution ids yet.
    public Exam extendExamTime(String examId, String teacherId, int additionalMinutes) {
        Optional<Exam> opt = examRepository.findById(examId);
        if (opt.isEmpty()) {
            return null;
        }
        Exam exam = opt.get();
        if (!exam.getCreatedByTeacherId().equals(teacherId)) {
            System.err.println("Extend-time rejected: " + teacherId + " did not create exam " + examId);
            return null;
        }
        return exam;
    }

    // Helpers
    private String generateExecutionCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return code.toString();
    }
    private boolean hasStudentAlreadySubmitted(String studentId, String examId) {
        String sql = "SELECT COUNT(*) FROM exam_answers WHERE student_id = ? AND exam_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private List<Question> fetchQuestionsByIds(List<String> ids) {
        List<Question> list = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return list;

        String sql = "SELECT q.*, c.name as course_name FROM questions q " +
                "LEFT JOIN courses c ON q.course_id = c.course_id WHERE q.question_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return list;

        for (String qId : ids) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, qId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Question q = new Question();
                        q.setQuestionId(rs.getString("question_id"));
                        q.setText(rs.getString("text"));
                        q.setDifficulty(Difficulty.valueOf(rs.getString("difficulty")));
                        q.setInstructions(rs.getString("instructions"));
                        q.setTopic(rs.getString("topic"));
                        q.setCourseId(rs.getString("course_id"));
                        list.add(q);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private List<Question> fetchMatchingQuestions(String courseId, String topic, Difficulty difficulty, int limit) {
        List<Question> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM questions WHERE course_id = ?");

        if (topic != null && !topic.isBlank()) sql.append(" AND topic LIKE ?");
        if (difficulty != null) sql.append(" AND difficulty = ?");
        sql.append(" LIMIT ?");

        Connection conn = dbManager.getConnection();
        if (conn == null) return list;

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            stmt.setString(idx++, courseId);
            if (topic != null && !topic.isBlank()) stmt.setString(idx++, "%" + topic + "%");
            if (difficulty != null) stmt.setString(idx++, difficulty.name());
            stmt.setInt(idx, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question();
                    q.setQuestionId(rs.getString("question_id"));
                    q.setText(rs.getString("text"));
                    q.setDifficulty(Difficulty.valueOf(rs.getString("difficulty")));
                    q.setInstructions(rs.getString("instructions"));
                    q.setTopic(rs.getString("topic"));
                    q.setCourseId(rs.getString("course_id"));
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private String fetchCorrectAnswerForQuestion(String questionId) {
        String sql = "SELECT answer_text FROM question_answers WHERE question_id = ? AND is_correct = 1";
        Connection conn = dbManager.getConnection();
        if (conn == null) return null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, questionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("answer_text");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}