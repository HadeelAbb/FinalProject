import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.net.dto.SubmitExamData;
import server.controllers.ExamServerController;
import server.db.DatabaseManager;

import java.util.HashMap;
import java.util.Map;

public class RepositoryTestDriver {

    public static void main(String[] args) {
        // 1. Connect to Database
        DatabaseManager db = DatabaseManager.getInstance();
        if (!db.connect()) {
            System.err.println("Database connection failed!");
            return;
        }

        // 2. Instantiate Controller
        ExamServerController controller = new ExamServerController();

        // 3. Prepare Simulated Student Answers for Exam E72874
        // Questions: 11001 to 11005 (20 points each)
        Map<String, String> studentAnswers = new HashMap<>();

        // Correct answers
        studentAnswers.put("11001", "Execute instructions and perform arithmetic/logic operations");
        studentAnswers.put("11002", "Stack");
        studentAnswers.put("11003", "Allows concurrent execution of tasks to maximize CPU utilization");
        studentAnswers.put("11004", "Referential Integrity between tables");

        // WRONG answer on purpose to test partial score (Expected: 80.0 / 100)
        studentAnswers.put("11005", "O(1)");

        SubmitExamData submitData = new SubmitExamData("E72874", "student1", studentAnswers, false);

        // 4. Execute Submission Logic
        System.out.println("\n--- SUBMITTING EXAM FOR AUTO-GRADING ---");
        ExamAnswer result = controller.submitExam(submitData);

        // 5. Assert Results
        if (result != null) {
            System.out.println("✅ SUBMISSION SUCCESSFUL!");
            System.out.println("Answer Record ID: " + result.getExamAnswerId());
            System.out.println("Student ID: " + result.getStudentId());
            System.out.println("Auto Score: " + result.getAutoScore() + " / 100.0");
            System.out.println("Final Score: " + result.getFinalScore());
        } else {
            System.out.println("❌ SUBMISSION FAILED (Check if student already submitted or exam window expired)");
        }

        db.disconnect();
    }
}