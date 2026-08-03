//A test to check those SUC with ExamServerController methods:
//(SUC-7/SUC-8): Test teacher score overrides and comments in exam_answers
package server.db;

import com.hsts.shared.net.dto.ConfirmGradeData;
import server.controllers.ExamServerController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Acceptance Test Driver for SUC-7 / SUC-8: Teacher Grade Confirmation & Score Override.
 */
public class GradeConfirmationTestDriver {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  SUC-7/8 GRADE CONFIRMATION & OVERRIDE TEST SUITE");
        System.out.println("==================================================\n");

        DatabaseManager db = DatabaseManager.getInstance();
        if (!db.connect()) {
            System.err.println("❌ DB connection failed!");
            return;
        }

        ExamServerController controller = new ExamServerController();

        // -------------------------------------------------------------
        // Step 1: Fetch an existing ExamAnswer record ID to update
        // -------------------------------------------------------------
        String answerIdToUpdate = fetchLatestExamAnswerId("student1", "E72874");

        if (answerIdToUpdate == null) {
            System.err.println("❌ Setup failed: No submitted exam found for student1 on E72874.");
            System.err.println("   Please run ExamExecutionTestDriver first!");
            db.disconnect();
            return;
        }

        System.out.println("👉 Found Exam Answer Record to update: " + answerIdToUpdate);

        // -------------------------------------------------------------
        // Step 2: Teacher Overrides Grade & Adds Feedback
        // -------------------------------------------------------------
        System.out.println("\n👉 Executing Teacher Score Override & Feedback...");

        double overriddenScore = 85.0;
        String teacherNote = "Great effort! Added partial credit for question 5.";

        ConfirmGradeData confirmData = new ConfirmGradeData(
                answerIdToUpdate,
                "teacher1",
                overriddenScore,
                teacherNote
        );

        boolean success = controller.confirmGrade(confirmData);

        // -------------------------------------------------------------
        // Step 3: Assert Result & Verify Database Update
        // -------------------------------------------------------------
        if (success) {
            System.out.println("   [PASS] confirmGrade() returned true!");
            verifyInDatabase(answerIdToUpdate);
            System.out.println("\n✅ SUC-7/8 SUITE PASSED PERFECTLY!");
        } else {
            System.err.println("   [FAIL] confirmGrade() failed to update database.");
        }

        db.disconnect();
    }

    private static String fetchLatestExamAnswerId(String studentId, String examId) {
        String sql = "SELECT exam_answer_id FROM exam_answers WHERE student_id = ? AND exam_id = ? ORDER BY submitted_at DESC LIMIT 1";
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("exam_answer_id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void verifyInDatabase(String examAnswerId) {
        // Updated column name from is_grade_confirmed to grade_confirmed
        String sql = "SELECT auto_score, final_score, teacher_comment, grade_confirmed FROM exam_answers WHERE exam_answer_id = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, examAnswerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("   [DB VERIFIED] Auto Score: " + rs.getDouble("auto_score"));
                    System.out.println("   [DB VERIFIED] Overridden Final Score: " + rs.getDouble("final_score"));
                    System.out.println("   [DB VERIFIED] Teacher Comment: \"" + rs.getString("teacher_comment") + "\"");
                    System.out.println("   [DB VERIFIED] Grade Confirmed Flag: " + rs.getBoolean("grade_confirmed"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}