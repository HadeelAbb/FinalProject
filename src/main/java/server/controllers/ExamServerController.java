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

    // SUC-6: Submit Exam & Auto-Grade Multiple Choice Options
    public ExamAnswer submitExam(SubmitExamData data) {
        Optional<Exam> optExam = examRepository.findById(data.getExamId());
        if (optExam.isEmpty()) return null;

        Exam exam = optExam.get();
        double earnedPoints = 0.0;
        int totalQuestions = exam.getQuestions().size();
        double pointsPerQuestion = totalQuestions > 0 ? (100.0 / totalQuestions) : 0.0;

        // Auto-grading routine
        for (Question q : exam.getQuestions()) {
            String studentSelected = data.getSelectedAnswers().get(q.getQuestionId());
            String correctAnswer = fetchCorrectAnswerForQuestion(q.getQuestionId());

            if (studentSelected != null && studentSelected.trim().equalsIgnoreCase(correctAnswer != null ? correctAnswer.trim() : "")) {
                earnedPoints += pointsPerQuestion;
            }
        }

        String newAnswerId = "EA" + (System.currentTimeMillis() % 100000);
        ExamAnswer answer = new ExamAnswer(newAnswerId, data.getExamId(), data.getStudentId());
        answer.setSelectedAnswers(data.getSelectedAnswers());
        answer.setSubmittedAt(LocalDateTime.now());
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
    private List<Question> fetchQuestionsByIds(List<String> ids) {
        List<Question> list = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return list;

        String sql = "SELECT q.*, c.name as course_name FROM questions q " +
                "LEFT JOIN courses c ON q.course_id = c.course_id WHERE q.question_id = ?";
        Connection conn = dbManager.getConnection();

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