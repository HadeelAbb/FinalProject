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
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    // SUC-2: Manual Exam Creation
    public Exam createManualExam(CreateExamManualData data) {
        String newExamId = "E" + (System.currentTimeMillis() % 100000);
        List<Question> selectedQuestions = fetchQuestionsByIds(data.getQuestionIds());

        Exam exam = new Exam(newExamId, data.getCourseId(), data.getTitle(),
                data.getInstructionsForStudents(), selectedQuestions, data.getDurationMinutes(), data.getTeacherId());
        exam.setStatus(ExamStatus.DRAFT);

        return examRepository.save(exam) ? exam : null;
    }

    // SUC-3: Auto Exam Creation by Topic & Difficulty
    public Exam createAutoExam(CreateExamAutoData data) {
        List<Question> matchingQuestions = fetchMatchingQuestions(
                data.getCourseId(), data.getTopic(), data.getDifficulty(), data.getNumberOfQuestions());

        if (matchingQuestions.size() < data.getNumberOfQuestions()) {
            return null; // Insufficient matching pool
        }

        String newExamId = "E" + (System.currentTimeMillis() % 100000);
        Exam exam = new Exam(newExamId, data.getCourseId(), data.getTitle(),
                data.getInstructionsForStudents(), matchingQuestions, data.getDurationMinutes(), data.getTeacherId());
        exam.setStatus(ExamStatus.DRAFT);

        return examRepository.save(exam) ? exam : null;
    }

    // SUC-4: Exam Approval
    public Exam approveExam(String examId, String coordinatorId) {
        Optional<Exam> opt = examRepository.findById(examId);
        if (opt.isPresent()) {
            Exam exam = opt.get();
            exam.setStatus(ExamStatus.APPROVED);
            exam.setApprovedByCoordinatorId(coordinatorId);
            exam.setScheduledStart(LocalDateTime.now());
            exam.setScheduledEnd(LocalDateTime.now().plusDays(14)); // Scheduled active window
            exam.setExecutionCode(generateExecutionCode());
            return examRepository.update(exam) ? exam : null;
        }
        return null;
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

        // 2. Validate 4-character execution code (case-insensitive)
        if (exam.getExecutionCode() != null && !exam.getExecutionCode().equalsIgnoreCase(submittedExecutionCode)) {
            System.err.println("Start rejected: Invalid execution code provided for exam " + data.getExamId());
            return null;
        }

        // 3. Check if student already submitted
        if (hasStudentAlreadySubmitted(data.getStudentId(), data.getExamId())) {
            System.err.println("Start rejected: Student " + data.getStudentId() + " already took this exam.");
            return null;
        }

        return exam; // Execution code verified; return exam payload
    }

    // SUC-6: Submit Exam & Auto-Grade Multiple Choice Options
    public ExamAnswer submitExam(SubmitExamData data) {
        Optional<Exam> optExam = examRepository.findById(data.getExamId());
        if (optExam.isEmpty()) return null;

        Exam exam = optExam.get();

        // 1. Check if student has already submitted this exam (Enforce 1 attempt per student)
        if (hasStudentAlreadySubmitted(data.getStudentId(), data.getExamId())) {
            System.err.println("Submission rejected: Student " + data.getStudentId() + " has already taken exam " + data.getExamId());
            return null;
        }

        // 2. Optional: Verify exam is currently active/open for submission
        LocalDateTime now = LocalDateTime.now();
        if (exam.getScheduledStart() != null && now.isBefore(exam.getScheduledStart())) {
            System.err.println("Submission rejected: Exam has not started yet.");
            return null;
        }
        if (exam.getScheduledEnd() != null && now.isAfter(exam.getScheduledEnd())) {
            System.err.println("Submission rejected: Exam window has expired.");
            return null;
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
        answer.setSelectedAnswers(data.getSelectedAnswers());
        answer.setSubmittedAt(now);
        answer.setAutoSubmitted(data.isAutoSubmitted());
        answer.setAutoScore(earnedPoints);
        answer.setFinalScore(earnedPoints); // Default until overridden by teacher
        answer.setGradeConfirmed(false);

        return answerRepository.save(answer) ? answer : null;
    }

    // SUC-7: Teacher Grade Confirmation / Score Override
    public boolean confirmGrade(ConfirmGradeData data) {
        Optional<ExamAnswer> opt = answerRepository.findById(data.getExamAnswerId());
        if (opt.isPresent()) {
            ExamAnswer answer = opt.get();
            answer.setFinalScore(data.getFinalScore());
            answer.setTeacherComment(data.getTeacherComment());
            answer.setGradeConfirmed(true);
            return answerRepository.update(answer);
        }
        return false;
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