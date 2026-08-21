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
    private final ActiveExamTracker activeExamTracker = new ActiveExamTracker();

    // SUC-2: Manual Exam Creation
    public Exam createManualExam(CreateExamManualData data) {
        return tryCreateManualExam(data).exam;
    }

    public CreateExamResult tryCreateManualExam(CreateExamManualData data) {
        if (!isTeacherAssignedToCourse(data.getTeacherId(), data.getCourseId())) {
            System.err.println("Create-exam rejected: teacher " + data.getTeacherId()
                    + " does not teach course " + data.getCourseId());
            return CreateExamResult.fail("You don't have permission to build exams for this course.");
        }

        String pointsError = ExamQuestionPointsValidator.validate(data.getQuestionIds(), data.getQuestionPoints());
        if (pointsError != null) {
            return CreateExamResult.fail(pointsError);
        }

        String newExamId = "E" + (System.currentTimeMillis() % 100000);
        List<Question> selectedQuestions = fetchQuestionsByIds(data.getQuestionIds());
        if (selectedQuestions.size() != data.getQuestionIds().size()) {
            return CreateExamResult.fail("One or more selected questions no longer exist.");
        }
        for (Question question : selectedQuestions) {
            question.setPoints(data.getQuestionPoints().get(question.getQuestionId()));
        }

        Exam exam = new Exam(newExamId, data.getCourseId(), data.getTitle(),
                data.getInstructionsForStudents(), selectedQuestions, data.getDurationMinutes(), data.getTeacherId());
        exam.setStatus(ExamStatus.DRAFT);
        exam.setInstructionsForTeacher(data.getInstructionsForTeacher());
        exam.setRootExamId(newExamId);
        exam.setVersionNumber(1);
        exam.setLatest(true);

        return examRepository.save(exam)
                ? CreateExamResult.ok(exam)
                : CreateExamResult.fail("Failed to create manual exam.");
    }

    // SUC-3: Auto Exam Creation by Topic & Difficulty
    public Exam createAutoExam(CreateExamAutoData data) {
        return tryCreateAutoExam(data).exam;
    }

    public CreateExamResult tryCreateAutoExam(CreateExamAutoData data) {
        if (!isTeacherAssignedToCourse(data.getTeacherId(), data.getCourseId())) {
            System.err.println("Create-exam rejected: teacher " + data.getTeacherId()
                    + " does not teach course " + data.getCourseId());
            return CreateExamResult.fail("You don't have permission to build exams for this course.");
        }

        String splitError = ExamQuestionPointsValidator.validateEqualSplit(data.getNumberOfQuestions());
        if (splitError != null) {
            return CreateExamResult.fail(splitError);
        }

        List<Question> matchingQuestions = fetchMatchingQuestions(
                data.getCourseId(), data.getTopic(), data.getDifficulty(), data.getNumberOfQuestions());

        if (matchingQuestions.size() < data.getNumberOfQuestions()) {
            return CreateExamResult.fail("Insufficient matching questions for criteria.");
        }

        int pointsEach = ExamQuestionPointsValidator.equalSplitPoints(matchingQuestions.size());
        for (Question question : matchingQuestions) {
            question.setPoints(pointsEach);
        }

        String newExamId = "E" + (System.currentTimeMillis() % 100000);
        Exam exam = new Exam(newExamId, data.getCourseId(), data.getTitle(),
                data.getInstructionsForStudents(), matchingQuestions, data.getDurationMinutes(), data.getTeacherId());
        exam.setStatus(ExamStatus.DRAFT);
        exam.setInstructionsForTeacher(data.getInstructionsForTeacher());
        exam.setRootExamId(newExamId);
        exam.setVersionNumber(1);
        exam.setLatest(true);

        return examRepository.save(exam)
                ? CreateExamResult.ok(exam)
                : CreateExamResult.fail("Failed to create automatic exam.");
    }

    public static final class CreateExamResult {
        public final Exam exam;
        public final String errorMessage;

        private CreateExamResult(Exam exam, String errorMessage) {
            this.exam = exam;
            this.errorMessage = errorMessage;
        }

        public static CreateExamResult ok(Exam exam) {
            return new CreateExamResult(exam, null);
        }

        public static CreateExamResult fail(String errorMessage) {
            return new CreateExamResult(null, errorMessage);
        }
    }

    public CreateExamResult tryCreateExamVersion(CreateExamVersionData data) {
        if (data == null || data.getSourceExamId() == null || data.getSourceExamId().isBlank()) {
            return CreateExamResult.fail(ExamVersioning.SOURCE_NOT_FOUND);
        }
        String titleError = ExamVersioning.validateTitle(data.getTitle());
        if (titleError != null) {
            return CreateExamResult.fail(titleError);
        }
        String durationError = ExamVersioning.validateDuration(data.getDurationMinutes());
        if (durationError != null) {
            return CreateExamResult.fail(durationError);
        }

        Optional<Exam> sourceOpt = examRepository.findById(data.getSourceExamId());
        if (sourceOpt.isEmpty()) {
            return CreateExamResult.fail(ExamVersioning.SOURCE_NOT_FOUND);
        }
        Exam source = sourceOpt.get();

        if (data.getTeacherId() == null || !data.getTeacherId().equals(source.getCreatedByTeacherId())) {
            return CreateExamResult.fail(RequestAuthorizer.NOT_AUTHORIZED);
        }
        if (!isTeacherAssignedToCourse(data.getTeacherId(), source.getCourseId())) {
            return CreateExamResult.fail("You don't have permission to build exams for this course.");
        }
        if (!source.isLatest()) {
            return CreateExamResult.fail(ExamVersioning.HISTORICAL_NOT_EDITABLE);
        }

        String pointsError = ExamQuestionPointsValidator.validate(data.getQuestionIds(), data.getQuestionPoints());
        if (pointsError != null) {
            return CreateExamResult.fail(pointsError);
        }

        List<Question> selectedQuestions = fetchQuestionsByIds(data.getQuestionIds());
        if (selectedQuestions.size() != data.getQuestionIds().size()) {
            return CreateExamResult.fail("One or more selected questions no longer exist.");
        }

        String latestError = ExamVersioning.validateNewlyAddedQuestionsAreLatest(
                ExamVersioning.physicalQuestionIds(source), selectedQuestions);
        if (latestError != null) {
            return CreateExamResult.fail(latestError);
        }

        for (Question question : selectedQuestions) {
            question.setPoints(data.getQuestionPoints().get(question.getQuestionId()));
        }

        String newExamId = allocateExamId();
        Exam exam = new Exam(newExamId, source.getCourseId(), data.getTitle(),
                data.getInstructionsForStudents(), selectedQuestions, data.getDurationMinutes(),
                source.getCreatedByTeacherId());
        exam.setStatus(ExamStatus.DRAFT);
        exam.setInstructionsForTeacher(data.getInstructionsForTeacher());
        exam.setRootExamId(source.getRootExamId());
        exam.setLatest(true);

        String saveError = examRepository.saveAsNewVersion(exam, source.getExamId());
        if (saveError != null) {
            return CreateExamResult.fail(saveError);
        }
        return CreateExamResult.ok(exam);
    }

    private String allocateExamId() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String id = "E" + ((System.currentTimeMillis() + attempt * 37L) % 100000);
            if (examRepository.findById(id).isEmpty()) {
                return id;
            }
        }
        return "E" + (System.nanoTime() % 100000);
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

    private LocalDateTime parseRequired(String text, java.time.format.DateTimeFormatter fmt) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), fmt);
        } catch (Exception e) {
            System.err.println("Could not parse date '" + text + "' (expected dd-MM-yyyy HH:mm).");
            return null;
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

        // Personal timer starts at THIS student's first successful START for this
        // execution, not at ExamExecution.scheduled_start. A later START of the
        // same execution resumes the original startedAt instead of resetting it.
        // Extra minutes are applied to remaining time without changing the stored
        // exam duration row.
        ActiveExamTracker.ActiveSitting sitting = activeExamTracker.markStarted(
                data.getStudentId(),
                exam.getCourseId(),
                exam.getExamId(),
                execution.getExecutionId(),
                now);
        if (sitting == null) {
            System.err.println("Start rejected: could not record active sitting.");
            return null;
        }
        int remainingSeconds = ActiveExamTracker.remainingSeconds(
                sitting.getStartedAt(),
                exam.getDurationMinutes(),
                execution.getExtraMinutesGranted(),
                now);
        exam.setRemainingSeconds(remainingSeconds);
        System.out.println("[EXAM-LOCK] " + data.getStudentId()
                + (sitting.getStartedAt() != now ? " resume: " : " marked active: ")
                + "exam=" + exam.getExamId()
                + " execution=" + execution.getExecutionId()
                + " course=" + exam.getCourseId()
                + " startedAt=" + sitting.getStartedAt()
                + " remainingSeconds=" + remainingSeconds);

        return exam;
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

        java.util.Map<String, String> officialCorrect = new java.util.HashMap<>();
        if (exam.getQuestions() != null) {
            for (Question q : exam.getQuestions()) {
                officialCorrect.put(q.getQuestionId(), fetchCorrectAnswerForQuestion(q.getQuestionId()));
            }
        }
        double earnedPoints = ExamQuestionPointsValidator.grade(
                exam.getQuestions(), data.getSelectedAnswers(), officialCorrect);

        String newAnswerId = "EA" + (System.currentTimeMillis() % 100000);
        ExamAnswer answer = new ExamAnswer(newAnswerId, data.getExamId(), data.getStudentId());
        answer.setExecutionId(execution != null ? execution.getExecutionId() : null);
        answer.setSelectedAnswers(data.getSelectedAnswers());
        answer.setSubmittedAt(LocalDateTime.now());
        answer.setAutoSubmitted(data.isAutoSubmitted());
        answer.setAutoScore(earnedPoints);
        answer.setFinalScore(earnedPoints); // Default until overridden by teacher
        answer.setGradeConfirmed(false);

        if (!answerRepository.save(answer)) {
            return null;
        }

        // Unlock the bot only after the submission row is actually persisted.
        activeExamTracker.clearByExam(data.getStudentId(), data.getExamId());
        System.out.println("[EXAM-LOCK] " + data.getStudentId()
                + " cleared after submit: exam=" + data.getExamId()
                + " course=" + exam.getCourseId());
        return answer;
    }
    // SUC-6: resolve a 4-character code through exam_executions, then load the approved exam.
    // Production student entry must not depend on the leftover exams.execution_code column.
    public Exam getExamByExecutionCode(String executionCode) {
        if (ExamExecutionCreateValidator.validateExecutionCode(executionCode) != null) {
            System.err.println("[EXAM-SERVER] Invalid execution code format.");
            return null;
        }

        Optional<com.hsts.shared.model.ExamExecution> executionOpt =
                executionRepository.findByCode(executionCode.trim());
        if (executionOpt.isEmpty()) {
            System.err.println("[EXAM-SERVER] No exam_executions row for code: " + executionCode);
            return null;
        }

        Optional<Exam> optExam = examRepository.findById(executionOpt.get().getExamId());
        if (optExam.isPresent() && optExam.get().getStatus() == ExamStatus.APPROVED) {
            System.out.println("[EXAM-SERVER] Fetched exam [" + optExam.get().getExamId()
                    + "] via exam_executions code: " + executionCode);
            return optExam.get();
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
        Optional<Exam> examOpt = examRepository.findById(answer.getExamId());

        if (data.getTeacherId() != null && examOpt.isPresent()
                && examOpt.get().getCreatedByTeacherId() != null
                && !data.getTeacherId().equals(examOpt.get().getCreatedByTeacherId())) {
            return RequestAuthorizer.NOT_AUTHORIZED;
        }

        String reasonError = GradeChangeReasonValidator.validate(
                answer.getAutoScore(), data.getFinalScore(), data.getTeacherComment());
        if (reasonError != null) {
            return reasonError;
        }

        double finalScore = data.getFinalScore();
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

    public Optional<ExamAnswer> findExamAnswerById(String examAnswerId) {
        return answerRepository.findById(examAnswerId);
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
        return submitForApproval(examId, null);
    }

    public Exam submitForApproval(String examId, String teacherId) {
        Optional<Exam> opt = examRepository.findById(examId);
        if (opt.isEmpty()) {
            return null;
        }
        Exam exam = opt.get();
        if (teacherId != null && exam.getCreatedByTeacherId() != null
                && !teacherId.equals(exam.getCreatedByTeacherId())) {
            System.err.println("Submit-for-approval rejected: " + teacherId
                    + " did not create exam " + examId);
            return null;
        }
        if (!exam.isLatest()) {
            System.err.println("Submit-for-approval rejected: exam " + examId + " is not the current version.");
            return null;
        }
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.REJECTED) {
            System.err.println("Submit-for-approval rejected: exam " + examId + " is not a draft or rejected exam.");
            return null;
        }
        String pointsError = ExamQuestionPointsValidator.validateQuestions(exam.getQuestions());
        if (pointsError != null) {
            System.err.println("Submit-for-approval rejected: " + pointsError);
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

    // SUC 7.3 / requirement 12: Principal comparison of related exams (confirmed grades only)
    public PrincipalComparisonReport getPrincipalComparisonReport(PrincipalReportType type, String filterValue) {
        return PrincipalReportAssembler.assemble(type, filterValue, examRepository.findAll(),
                answerRepository.findAllConfirmed());
    }

    // SUC 2.2: a teacher opens ANOTHER execution (sitting) of an already-approved exam
    // that they created. Spec 7.2 in this codebase is about exam statistics for
    // authorized viewers, not a license for any teacher to operate another teacher's
    // exam. The GUI lists GET_MY_EXAMS only; the server must enforce the same ownership.
    public com.hsts.shared.model.ExamExecution createExecution(String examId, String teacherId,
                                                               String scheduledStartText, String scheduledEndText,
                                                               String executionCode) {
        CreateExecutionResult result = tryCreateExecution(examId, teacherId, scheduledStartText, scheduledEndText,
                executionCode);
        return result.execution;
    }

    /**
     * Same as createExecution, but returns a specific failure message for the OCSF client.
     */
    public CreateExecutionResult tryCreateExecution(String examId, String teacherId,
                                                    String scheduledStartText, String scheduledEndText,
                                                    String executionCode) {
        Optional<Exam> opt = examRepository.findById(examId);
        if (opt.isEmpty()) {
            return CreateExecutionResult.fail("Exam not found.");
        }
        Exam exam = opt.get();
        String ownerError = ExamResultsAccess.denyIfNotOwner(exam, teacherId);
        if (ownerError != null) {
            return CreateExecutionResult.fail(ExamResultsAccess.NOT_FOUND.equals(ownerError)
                    ? ExamResultsAccess.NOT_FOUND
                    : ExamResultsAccess.ACCESS_DENIED);
        }
        String approvedError = ExamExecutionCreateValidator.validateApproved(exam.getStatus());
        if (approvedError != null) {
            return CreateExecutionResult.fail(approvedError);
        }

        if (scheduledStartText == null || scheduledStartText.isBlank()
                || scheduledEndText == null || scheduledEndText.isBlank()) {
            return CreateExecutionResult.fail("Opening and closing times are required.");
        }

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        LocalDateTime start = parseRequired(scheduledStartText, fmt);
        LocalDateTime end = parseRequired(scheduledEndText, fmt);
        if (start == null || end == null) {
            return CreateExecutionResult.fail("Opening and closing times must use the format dd-MM-yyyy HH:mm.");
        }

        String windowError = ExamExecutionCreateValidator.validateWindow(start, end);
        if (windowError != null) {
            return CreateExecutionResult.fail(windowError);
        }

        String codeError = ExamExecutionCreateValidator.validateExecutionCode(executionCode);
        if (codeError != null) {
            return CreateExecutionResult.fail(codeError);
        }
        String code = ExamExecutionCreateValidator.normalizeExecutionCode(executionCode);
        if (!executionRepository.findByCode(code).isEmpty()) {
            return CreateExecutionResult.fail("Execution code is already in use.");
        }

        String executionId = "EX" + (System.currentTimeMillis() % 100000);
        com.hsts.shared.model.ExamExecution execution = new com.hsts.shared.model.ExamExecution(
                executionId, examId, code, start, end, teacherId);
        if (!executionRepository.save(execution)) {
            return CreateExecutionResult.fail("Execution code is already in use.");
        }
        return CreateExecutionResult.ok(execution);
    }

    public static final class CreateExecutionResult {
        public final com.hsts.shared.model.ExamExecution execution;
        public final String errorMessage;

        private CreateExecutionResult(com.hsts.shared.model.ExamExecution execution, String errorMessage) {
            this.execution = execution;
            this.errorMessage = errorMessage;
        }

        public static CreateExecutionResult ok(com.hsts.shared.model.ExamExecution execution) {
            return new CreateExecutionResult(execution, null);
        }

        public static CreateExecutionResult fail(String errorMessage) {
            return new CreateExecutionResult(null, errorMessage);
        }
    }

    // Section 4: every execution (past and present) this exam has ever had.
    public List<com.hsts.shared.model.ExamExecution> getExecutionsForExam(String examId) {
        return executionRepository.findByExamId(examId);
    }

    // Section 4: started/finished/timed-out counts for one specific execution.
    public com.hsts.shared.model.ExecutionStats getExecutionStats(String executionId) {
        return answerRepository.getExecutionStats(executionId);
    }

    public java.util.Optional<com.hsts.shared.model.ExamExecution> findExecutionById(String executionId) {
        if (executionId == null || executionId.isBlank()) {
            return java.util.Optional.empty();
        }
        return executionRepository.findById(executionId);
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

    public boolean hasActiveExamInCourse(String studentId, String courseId) {
        return activeExamTracker.isActiveInCourse(studentId, courseId);
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

    /**
     * Submitted results for one exam, only if {@code teacherId} created that exam.
     * Returns an empty list when the exam exists and is owned but has no submissions.
     * Returns null when the exam is missing or not owned by this teacher.
     */
    public List<ExamAnswer> getExamResults(String examId, String teacherId) {
        Optional<Exam> opt = examRepository.findById(examId);
        Exam exam = opt.orElse(null);
        if (ExamResultsAccess.denyIfNotOwner(exam, teacherId) != null) {
            return null;
        }
        return answerRepository.findSubmittedByExamId(examId);
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
    // on - not just students already connected the instant this is called.
    // Only the teacher who created the exam may extend; spec 7.2 is exam statistics,
    // not cross-teacher execution control. Ownership is enforced from the
    // authenticated session (execution → exam → createdByTeacherId).
    public com.hsts.shared.model.ExamExecution extendExecutionTime(String executionId, String teacherId,
                                                                  int additionalMinutes) {
        Optional<com.hsts.shared.model.ExamExecution> opt = executionRepository.findById(executionId);
        if (opt.isEmpty()) {
            return null;
        }
        Optional<Exam> exam = examRepository.findById(opt.get().getExamId());
        if (ExamResultsAccess.denyIfNotOwner(exam.orElse(null), teacherId) != null) {
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
        String chars = ExamExecutionCreateValidator.CODE_ALPHABET;
        StringBuilder code = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return code.toString();
    }

    private String generateUniqueExecutionCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = generateExecutionCode();
            if (executionRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        return null;
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
        QuestionServerController.ensureVersionColumns(conn);

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
                        String rootQuestionId = rs.getString("root_question_id");
                        q.setRootQuestionId(rootQuestionId);
                        int qVersion = rs.getInt("version_number");
                        q.setVersionNumber(rs.wasNull() || qVersion < 1 ? 1 : qVersion);
                        q.setLatest(rs.getInt("is_latest") == 1);
                        QuestionIllustration.apply(q, rs.getBytes("image_data"), rs.getString("image_filename"));
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
        StringBuilder sql = new StringBuilder("SELECT * FROM questions WHERE course_id = ? AND is_latest = 1");

        if (topic != null && !topic.isBlank()) sql.append(" AND topic LIKE ?");
        if (difficulty != null) sql.append(" AND difficulty = ?");
        sql.append(" LIMIT ?");

        Connection conn = dbManager.getConnection();
        if (conn == null) return list;
        QuestionServerController.ensureVersionColumns(conn);

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
                    QuestionIllustration.apply(q, rs.getBytes("image_data"), rs.getString("image_filename"));
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