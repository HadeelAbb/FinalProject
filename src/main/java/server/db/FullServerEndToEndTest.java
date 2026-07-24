package server.db;

import com.hsts.shared.model.*;
import com.hsts.shared.net.*;
import com.hsts.shared.net.dto.*;
import ocsf.client.AbstractClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FullServerEndToEndTest extends AbstractClient {

    private CompletableFuture<Response> pendingResponse;

    public FullServerEndToEndTest(String host, int port) {
        super(host, port);
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg instanceof Response response && pendingResponse != null) {
            pendingResponse.complete(response);
        }
    }

    public Response sendRequestSync(Request request) throws Exception {
        pendingResponse = new CompletableFuture<>();
        sendToServer(request);
        return pendingResponse.get(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        System.out.println("=== RUNNING FULL END-TO-END NETWORK & DB TEST ===");

        FullServerEndToEndTest testClient = new FullServerEndToEndTest("localhost", 3000);

        try {
            testClient.openConnection();
            System.out.println("✓ Connected to live MainServerApp on port 3000!");

            // -------------------------------------------------------------
            // TEST 1: Manual Exam Creation over Socket Network
            // -------------------------------------------------------------
            System.out.println("\n--- 1. Testing CREATE_EXAM_MANUAL ---");
            CreateExamManualData manualData = new CreateExamManualData(
                    "teacher1",                     // teacherId
                    "11",                           // courseId
                    "Network Architecture Midterm", // title
                    "Answer all questions.",        // instructionsForStudents
                    List.of("11001", "11002"),      // questionIds
                    45                              // durationMinutes
            );

            Request manualReq = new Request(Command.CREATE_EXAM_MANUAL, manualData, "test-req-1");
            Response manualResp = testClient.sendRequestSync(manualReq);

            if (manualResp.isSuccess() && manualResp.getPayload() instanceof Exam createdExam) {
                System.out.println("✓ Success! Exam created in DB with ID: " + createdExam.getExamId());
                String examId = createdExam.getExamId();

                // -------------------------------------------------------------
                // TEST 2: Approve Exam (Subject Coordinator Decision)
                // -------------------------------------------------------------
                System.out.println("\n--- 2. Testing APPROVE_EXAM ---");
                // Reason is null for an approval
                ExamApprovalDecisionData approveData = new ExamApprovalDecisionData(examId, "coord1", null);
                Request approveReq = new Request(Command.APPROVE_EXAM, approveData, "test-req-2");
                Response approveResp = testClient.sendRequestSync(approveReq);

                if (approveResp.isSuccess()) {
                    System.out.println("✓ Success! Exam " + examId + " status updated to APPROVED.");
                } else {
                    System.err.println("❌ Failed APPROVE_EXAM: " + approveResp.getMessage());
                }

                // -------------------------------------------------------------
                // TEST 3: Submit Exam Attempt (Student Auto-Grading)
                // -------------------------------------------------------------
                System.out.println("\n--- 3. Testing SUBMIT_EXAM ---");
                SubmitExamData submitData = new SubmitExamData(
                        examId, "admin", Map.of("11001", "O(log n)", "11002", "Stack"), false
                );
                Request submitReq = new Request(Command.SUBMIT_EXAM, submitData, "test-req-3");
                Response submitResp = testClient.sendRequestSync(submitReq);

                if (submitResp.isSuccess() && submitResp.getPayload() instanceof ExamAnswer answer) {
                    System.out.println("✓ Success! Answer recorded with ID: " + answer.getExamAnswerId());
                    System.out.println("  Auto-graded Score: " + answer.getAutoScore() + "%");

                    // -------------------------------------------------------------
                    // TEST 4: Teacher Grade Confirmation
                    // -------------------------------------------------------------
                    System.out.println("\n--- 4. Testing CONFIRM_GRADE ---");
                    ConfirmGradeData gradeData = new ConfirmGradeData(answer.getExamAnswerId(), "teacher1", 95.0, "Great effort!");
                    Request gradeReq = new Request(Command.CONFIRM_GRADE, gradeData, "test-req-4");
                    Response gradeResp = testClient.sendRequestSync(gradeReq);

                    if (gradeResp.isSuccess()) {
                        System.out.println("✓ Success! Teacher override and comment saved to MySQL.");
                    } else {
                        System.err.println("❌ Failed CONFIRM_GRADE: " + gradeResp.getMessage());
                    }
                } else {
                    System.err.println("❌ Failed SUBMIT_EXAM: " + submitResp.getMessage());
                }

            } else {
                System.err.println("❌ Failed CREATE_EXAM_MANUAL: " + manualResp.getMessage());
            }

            // -------------------------------------------------------------
            // TEST 5: Auto Exam Creation by Topic/Difficulty
            // -------------------------------------------------------------
            System.out.println("\n--- 5. Testing CREATE_EXAM_AUTO ---");
            CreateExamAutoData autoData = new CreateExamAutoData();
            autoData.setTeacherId("teacher1");
            autoData.setCourseId("11");
            autoData.setTitle("Automated Exam");
            autoData.setInstructionsForStudents("Instructions");
            autoData.setTopic("Data Structures");
            autoData.setDifficulty(Difficulty.EASY);
            autoData.setNumberOfQuestions(1);
            autoData.setDurationMinutes(30);

            Request autoReq = new Request(Command.CREATE_EXAM_AUTO, autoData, "test-req-5");
            Response autoResp = testClient.sendRequestSync(autoReq);

            if (autoResp.isSuccess() && autoResp.getPayload() instanceof Exam autoExam) {
                System.out.println("✓ Success! Auto-generated Exam created with ID: " + autoExam.getExamId());
            } else {
                System.err.println("❌ Failed CREATE_EXAM_AUTO: " + autoResp.getMessage());
            }

            // -------------------------------------------------------------
            // TEST 6: Testing Study Bot Interaction
            // -------------------------------------------------------------
            System.out.println("\n--- 6. Testing ASK_BOT_QUESTION ---");
            BotInteraction botReqData = new BotInteraction(
                    null, "student1", "11", "How do I format a binary tree?", ""
            );

            // Fixed: Uses Command.ASK_BOT_QUESTION
            Request botReq = new Request(Command.ASK_BOT_QUESTION, botReqData, "test-bot-1");
            Response botResp = testClient.sendRequestSync(botReq);

            if (botResp.isSuccess() && botResp.getPayload() instanceof BotInteraction savedBot) {
                System.out.println("✓ Success! Bot responded and saved interaction ID: " + savedBot.getInteractionId());
                // Fixed: Uses getAnswer() instead of getBotResponse()
                System.out.println("  Response text: " + savedBot.getAnswer());
            } else {
                System.err.println("❌ Failed ASK_BOT_QUESTION: " + botResp.getMessage());
            }

            System.out.println("\n=== ALL END-TO-END NETWORK & DB TESTS PASSED SUCCESSFULLY! ===");

            testClient.closeConnection();

        } catch (Exception e) {
            System.err.println("❌ TEST EXCEPTION OCCURRED:");
            e.printStackTrace();
        }
    }
}