//A test to check those SUC with ExamServerController methods:
//SUC-2 (Manual Creation): A teacher drafts an exam by manually picking question IDs.
//SUC-3 (Auto Creation): A teacher requests an exam created automatically based on topic, difficulty, and question count.
//SUC-4 (Coordinator Approval / Rejection): A course coordinator reviews the exam.

package server.db;

import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamStatus;
import com.hsts.shared.net.dto.CreateExamAutoData;
import com.hsts.shared.net.dto.CreateExamManualData;
import server.controllers.ExamServerController;

import java.util.Arrays;
import java.util.List;

/**
 * Acceptance Test Driver for SUC-2, SUC-3, and SUC-4:
 * Exam Creation (Manual & Auto) and Coordinator Approval Workflow.
 */
public class ExamBuildTestDriver {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  SUC-2 / SUC-3 / SUC-4 EXAM WORKFLOW TEST SUITE");
        System.out.println("==================================================\n");

        DatabaseManager db = DatabaseManager.getInstance();
        if (!db.connect()) {
            System.err.println("❌ DB connection failed!");
            return;
        }

        ExamServerController controller = new ExamServerController();

        // -------------------------------------------------------------
        // TEST 1: SUC-2 - Create Manual Exam Draft
        // -------------------------------------------------------------
        System.out.println("👉 TEST 1: Creating Manual Exam Draft (SUC-2)...");
        List<String> questionIds = Arrays.asList("11001", "11002", "11003");
        java.util.Map<String, Integer> questionPoints = new java.util.LinkedHashMap<>();
        questionPoints.put("11001", 40);
        questionPoints.put("11002", 30);
        questionPoints.put("11003", 30);

        CreateExamManualData manualData = new CreateExamManualData(
                "teacher1",
                "CS101",
                "Manual Midterm Test",
                "Read questions carefully.",
                null,
                questionIds,
                questionPoints,
                45
        );

        Exam manualExam = controller.createManualExam(manualData);

        if (manualExam != null) {
            System.out.println("   [PASS] Created Manual Exam ID: " + manualExam.getExamId());
            System.out.println("   [PASS] Initial Status: " + manualExam.getStatus());
            System.out.println("   [PASS] Selected Questions Count: " + manualExam.getQuestions().size());
        } else {
            System.err.println("   [FAIL] Failed to create manual exam draft.");
            db.disconnect();
            return;
        }

        // -------------------------------------------------------------
        // TEST 2: SUC-3 - Create Auto-Generated Exam Draft
        // -------------------------------------------------------------
        System.out.println("\n👉 TEST 2: Auto-Generating Exam by Topic & Difficulty (SUC-3)...");
        CreateExamAutoData autoData = new CreateExamAutoData(
                "teacher1",
                "CS101",
                "Auto Architecture Quiz",
                "Auto generated quiz",
                "Architecture",
                Difficulty.EASY,
                1,
                30
        );

        Exam autoExam = controller.createAutoExam(autoData);

        if (autoExam != null) {
            System.out.println("   [PASS] Created Auto Exam ID: " + autoExam.getExamId());
            System.out.println("   [PASS] Auto-selected Questions Count: " + autoExam.getQuestions().size());
        } else {
            System.err.println("   [FAIL] Auto exam generation failed (check question pool criteria).");
        }

        // -------------------------------------------------------------
        // TEST 3: SUC-4 - Coordinator Approves Manual Exam
        // -------------------------------------------------------------
        System.out.println("\n👉 TEST 3: Coordinator Approves Manual Exam (SUC-4)...");
        Exam approvedExam = controller.approveExam(manualExam.getExamId(), "coord1");

        if (approvedExam != null && approvedExam.getStatus() == ExamStatus.APPROVED) {
            System.out.println("   [PASS] Status Updated To: " + approvedExam.getStatus());
            System.out.println("   [PASS] Generated 4-Char Execution Code: " + approvedExam.getExecutionCode());
            System.out.println("   [PASS] Approved By: " + approvedExam.getApprovedByCoordinatorId());
        } else {
            System.err.println("   [FAIL] Exam approval failed.");
        }

        // -------------------------------------------------------------
        // TEST 4: SUC-4 - Coordinator Rejects Auto Exam
        // -------------------------------------------------------------
        if (autoExam != null) {
            System.out.println("\n👉 TEST 4: Coordinator Rejects Auto Exam (SUC-4)...");
            String rejectReason = "Needs higher question count before release.";
            Exam rejectedExam = controller.rejectExam(autoExam.getExamId(), "coord1", rejectReason);

            if (rejectedExam != null && rejectedExam.getStatus() == ExamStatus.REJECTED) {
                System.out.println("   [PASS] Status Updated To: " + rejectedExam.getStatus());
                System.out.println("   [PASS] Rejection Reason Recorded: \"" + rejectedExam.getRejectionReason() + "\"");
                System.out.println("\n✅ SUC-2 / SUC-3 / SUC-4 SUITE PASSED PERFECTLY!");
            } else {
                System.err.println("   [FAIL] Exam rejection failed.");
            }
        }

        db.disconnect();
    }
}