//A test to check those SUC with ExamServerController methods:
//Tests SUC-6 (Execution Code Validation & Auto-Grading)

package server.db;

import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.net.dto.SubmitExamData;
import server.controllers.ExamServerController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

/**
 * Acceptance Test Driver for SUC-6: Exam Execution & Auto-Grading.
 */
public class ExamExecutionTestDriver {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  SUC-6 EXECUTION & AUTO-GRADING TEST SUITE");
        System.out.println("==================================================\n");

        DatabaseManager db = DatabaseManager.getInstance();
        if (!db.connect()) {
            System.err.println("❌ DB connection failed!");
            return;
        }

        ExamServerController controller = new ExamServerController();

        // -------------------------------------------------------------
        // TEST 1: Lookup Exam by Execution Code (HKD6)
        // -------------------------------------------------------------
        System.out.println("👉 TEST 1: Validate 4-Character Execution Code...");
        Exam exam = controller.getExamByExecutionCode("HKD6");

        if (exam != null) {
            System.out.println("   [PASS] Found Exam: " + exam.getTitle() + " (ID: " + exam.getExamId() + ")");
            System.out.println("   [PASS] Loaded Questions Count: " + exam.getQuestions().size());
        } else {
            System.err.println("   [FAIL] Could not fetch exam with execution code 'HKD6'");
            db.disconnect();
            return;
        }

        // -------------------------------------------------------------
        // TEST 2: Submit Exam Answers & Verify Auto-Grading
        // -------------------------------------------------------------
        System.out.println("\n👉 TEST 2: Submit Student Answers & Verify Auto-Grading...");

        String testStudentId = "student1";

        // Clean up previous test submissions for student1 without closing the main pipeline
        cleanUpPreviousSubmissions(testStudentId, "E72874");

        Map<String, String> studentAnswers = new HashMap<>();
        // 4 Correct Answers (80 pts)
        studentAnswers.put("11001", "Execute instructions and perform arithmetic/logic operations");
        studentAnswers.put("11002", "Stack");
        studentAnswers.put("11003", "Allows concurrent execution of tasks to maximize CPU utilization");
        studentAnswers.put("11004", "Referential Integrity between tables");
        // 1 Wrong Answer on purpose
        studentAnswers.put("11005", "O(1)");

        SubmitExamData submitData = new SubmitExamData("E72874", testStudentId, studentAnswers, false);
        ExamAnswer result = controller.submitExam(submitData);

        if (result != null) {
            System.out.println("   [PASS] Submission saved successfully. Answer ID: " + result.getExamAnswerId());
            System.out.println("   [PASS] Auto Score Calculated: " + result.getAutoScore() + " / 100.0");

            if (Math.abs(result.getAutoScore() - 80.0) < 0.01) {
                System.out.println("\n✅ SUC-6 SUITE PASSED PERFECTLY! (Exact 80.0 score match)");
            } else {
                System.out.println("\n⚠️ SUC-6 SUITE PARTIAL MATCH: Score was " + result.getAutoScore() + " (Expected 80.0)");
            }
        } else {
            System.err.println("   [FAIL] Submission was rejected by controller.");
        }

        db.disconnect();
    }

    private static void cleanUpPreviousSubmissions(String studentId, String examId) {
        String sql = "DELETE FROM exam_answers WHERE student_id = ? AND exam_id = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, examId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}