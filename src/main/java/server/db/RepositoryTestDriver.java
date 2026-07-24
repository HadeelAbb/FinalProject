package server.db;

import com.hsts.shared.model.*;
import server.db.repository.ExamAnswerRepositoryImpl;
import server.db.repository.ExamRepositoryImpl;

import java.time.LocalDateTime;
import java.util.List;

public class RepositoryTestDriver {

    public static void main(String[] args) {
        System.out.println("=== STARTING REPOSITORY & DB INTEGRATION TEST ===");

        // 1. Connect to MySQL Database
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.connect()) {
            System.err.println(">>> FATAL: Database connection failed. Aborting test.");
            return;
        }

        ExamRepositoryImpl examRepo = new ExamRepositoryImpl();
        ExamAnswerRepositoryImpl answerRepo = new ExamAnswerRepositoryImpl();

        try {
            // -------------------------------------------------------------
            // TEST 1: Create and Save a Test Exam
            // -------------------------------------------------------------
            System.out.println("\n--- TEST 1: Saving a Test Exam ---");

            // Build dummy question using seeded question ID "11001" from your DB
            Question testQuestion = new Question();
            testQuestion.setQuestionId("11001");

            Exam testExam = new Exam("E100", "11", "Midterm Exam 2026",
                    "Answer all questions carefully.", List.of(testQuestion), 60, "teacher1");
            testExam.setStatus(ExamStatus.DRAFT);

            boolean savedExam = examRepo.save(testExam);
            System.out.println("Exam saved to MySQL: " + savedExam);

            // -------------------------------------------------------------
            // TEST 2: Retrieve Exam by ID from Database
            // -------------------------------------------------------------
            System.out.println("\n--- TEST 2: Reading Exam 'E100' back from DB ---");
            examRepo.findById("E100").ifPresentOrElse(
                    exam -> {
                        System.out.println("✓ Found Exam: " + exam.getTitle() + " | Status: " + exam.getStatus());
                        System.out.println("  Linked Questions Count: " + exam.getQuestions().size());
                    },
                    () -> System.err.println("❌ Failed to find exam 'E100'!")
            );

            // -------------------------------------------------------------
            // TEST 3: Update Exam Status (Approval Simulation)
            // -------------------------------------------------------------
            System.out.println("\n--- TEST 3: Updating Exam Status to APPROVED ---");
            testExam.setStatus(ExamStatus.APPROVED);
            testExam.setApprovedByCoordinatorId("coord1");
            boolean updatedExam = examRepo.update(testExam);
            System.out.println("Exam update status: " + updatedExam);

            // -------------------------------------------------------------
            // TEST 4: Record Student Exam Submission
            // -------------------------------------------------------------
            System.out.println("\n--- TEST 4: Saving Student Submission ---");
            // Uses student username 'admin' or any valid user in your users table
            ExamAnswer studentAttempt = new ExamAnswer("EA100", "E100", "admin");
            studentAttempt.setStartedAt(LocalDateTime.now().minusMinutes(30));
            studentAttempt.setSubmittedAt(LocalDateTime.now());
            studentAttempt.setAutoScore(85.0);
            studentAttempt.getSelectedAnswers().put("11001", "O(log n)");

            boolean savedAnswer = answerRepo.save(studentAttempt);
            System.out.println("Student Answer saved: " + savedAnswer);

            // -------------------------------------------------------------
            // TEST 5: Update Grade & Confirm
            // -------------------------------------------------------------
            System.out.println("\n--- TEST 5: Confirming Grade ---");
            studentAttempt.setFinalScore(90.0);
            studentAttempt.setTeacherComment("Great work on question 1!");
            studentAttempt.setGradeConfirmed(true);
            boolean updatedAnswer = answerRepo.update(studentAttempt);
            System.out.println("Grade update confirmed: " + updatedAnswer);

            // Read answer back
            answerRepo.findById("EA100").ifPresent(ans ->
                    System.out.println("✓ Read back Answer EA100: Final Score = " + ans.getFinalScore()
                            + " | Comment = " + ans.getTeacherComment())
            );

            // -------------------------------------------------------------
            // CLEANUP: Delete test records
            // -------------------------------------------------------------
            System.out.println("\n--- CLEANUP: Removing Test Data ---");
            boolean deletedAnswer = answerRepo.deleteById("EA100");
            boolean deletedExam = examRepo.deleteById("E100");
            System.out.println("Deleted test answer EA100: " + deletedAnswer);
            System.out.println("Deleted test exam E100: " + deletedExam);

            System.out.println("\n=== ALL REPOSITORY TESTS PASSED SUCCESSFULLY! ===");

        } catch (Exception e) {
            System.err.println("❌ TEST FAILED WITH EXCEPTION:");
            e.printStackTrace();
        } finally {
            dbManager.disconnect();
        }
    }
}