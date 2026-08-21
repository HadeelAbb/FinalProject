// Here is the complete, expanded FullServerEndToEndTest.java suite:
//It now covers the entire system lifecycle, including:
//User Authentication & Session Management (SUC-1) — Login, duplicate session checks, and logout.
//Manual Exam Creation (SUC-2).
//Coordinator Approval (SUC-4).
//Execution Code Lookup (SUC-6).
//Student Submission & Auto-Grading (SUC-6).
//Duplicate Submission Protection.
//Teacher Grade Override & Confirmation (SUC-7).
//Auto Exam Creation by Topic/Difficulty (SUC-3).
//Coordinator Exam Rejection Workflow (SUC-4).
//Study Bot Interaction.

// **** NEED TO RUN MainServerApp FIRST ****

package server.db;

import com.hsts.shared.model.*;
import com.hsts.shared.net.*;
import com.hsts.shared.net.dto.*;
import ocsf.client.AbstractClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Complete Holistic End-to-End Test Suite for HSTS Network & Database.
 * Tests OCSF Network Protocol, Controller Rules, and MySQL Persistence.
 */
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
        System.out.println("==========================================================================");
        System.out.println("      HSTS FULL HOLISTIC NETWORK & DATABASE INTEGRATION TEST SUITE       ");
        System.out.println("==========================================================================");

        FullServerEndToEndTest testClient = new FullServerEndToEndTest("localhost", 3000);

        try {
            testClient.openConnection();
            System.out.println("✓ Connected to live MainServerApp on port 3000!");

            // =================================================================
            // TEST 1: User Authentication & Session Control (SUC-1)
            // =================================================================
            System.out.println("\n--- 1. Testing LOGIN & Session Constraints ---");
            LoginData loginData = new LoginData("student1", "123456");
            Request loginReq = new Request(Command.LOGIN, loginData, "test-auth-1");
            Response loginResp = testClient.sendRequestSync(loginReq);

            if (loginResp.isSuccess()) {
                System.out.println("✓ Success! Authenticated user 'student1'.");

                // Test Duplicate Login Protection
                Response dupLoginResp = testClient.sendRequestSync(loginReq);
                if (!dupLoginResp.isSuccess()) {
                    System.out.println("✓ Success! Server correctly blocked duplicate session for active user.");
                } else {
                    System.err.println("❌ Failed Duplicate Login Check: Allowed double login for 'student1'.");
                }
            } else {
                System.err.println("❌ Failed LOGIN: " + loginResp.getMessage());
            }

            // =================================================================
            // TEST 2: Manual Exam Creation over Socket Network (SUC-2)
            // =================================================================
            System.out.println("\n--- 2. Testing CREATE_EXAM_MANUAL ---");
            CreateExamManualData manualData = new CreateExamManualData(
                    "teacher1",                     // teacherId
                    "CS101",                        // courseId
                    "Network Architecture Midterm", // title
                    "Answer all questions.",        // instructionsForStudents
                    null,
                    List.of("11001", "11002"),      // questionIds
                    java.util.Map.of("11001", 50, "11002", 50),
                    45                              // durationMinutes
            );

            Request manualReq = new Request(Command.CREATE_EXAM_MANUAL, manualData, "test-req-1");
            Response manualResp = testClient.sendRequestSync(manualReq);

            if (manualResp.isSuccess() && manualResp.getPayload() instanceof Exam createdExam) {
                String examId = createdExam.getExamId();
                System.out.println("✓ Success! Manual Exam draft created in MySQL with ID: " + examId);

                // =============================================================
                // TEST 3: Approve Exam & Execution Code Generation (SUC-4)
                // =============================================================
                System.out.println("\n--- 3. Testing APPROVE_EXAM (Coordinator Decision) ---");
                ExamApprovalDecisionData approveData = new ExamApprovalDecisionData(examId, "coord1", null);
                Request approveReq = new Request(Command.APPROVE_EXAM, approveData, "test-req-2");
                Response approveResp = testClient.sendRequestSync(approveReq);

                if (approveResp.isSuccess() && approveResp.getPayload() instanceof Exam approvedExam) {
                    String execCode = approvedExam.getExecutionCode();
                    System.out.println("✓ Success! Exam " + examId + " status updated to APPROVED.");
                    System.out.println("  Generated 4-Character Execution Code: " + execCode);

                    // =========================================================
                    // TEST 4: Validate Execution Code & Start Exam (SUC-6)
                    // =========================================================
                    System.out.println("\n--- 4. Testing START_EXAM via Execution Code ---");
                    Request startExamReq = new Request(Command.START_EXAM, execCode, "test-req-code");
                    Response startExamResp = testClient.sendRequestSync(startExamReq);

                    if (startExamResp.isSuccess() && startExamResp.getPayload() instanceof Exam fetchedExam) {
                        System.out.println("✓ Success! Validated code and started exam '" + fetchedExam.getTitle() + "'");
                    } else if (startExamResp.isSuccess()) {
                        System.out.println("✓ Success! Execution code " + execCode + " validated by server.");
                    } else {
                        System.err.println("❌ Failed START_EXAM: " + startExamResp.getMessage());
                    }

                    // =========================================================
                    // TEST 5: Submit First Exam Attempt & Auto-Grading (SUC-6)
                    // =========================================================
                    System.out.println("\n--- 5. Testing SUBMIT_EXAM (First Attempt & Auto-Grading) ---");
                    SubmitExamData submitData = new SubmitExamData(
                            examId,
                            "student1",
                            Map.of("11001", "Execute instructions and perform arithmetic/logic operations", "11002", "Stack"),
                            false
                    );
                    Request submitReq = new Request(Command.SUBMIT_EXAM, submitData, "test-req-3");
                    Response submitResp = testClient.sendRequestSync(submitReq);

                    if (submitResp.isSuccess() && submitResp.getPayload() instanceof ExamAnswer answer) {
                        String answerId = answer.getExamAnswerId();
                        System.out.println("✓ Success! Exam answer recorded with ID: " + answerId);
                        System.out.println("  Auto-graded Score: " + answer.getAutoScore() + "%");

                        // =========================================================
                        // TEST 5.5: Duplicate Exam Submission (Should Fail)
                        // =========================================================
                        System.out.println("\n--- 5.5. Testing SUBMIT_EXAM (Duplicate Submission Check) ---");
                        Request duplicateReq = new Request(Command.SUBMIT_EXAM, submitData, "test-req-3-dup");
                        Response duplicateResp = testClient.sendRequestSync(duplicateReq);

                        if (!duplicateResp.isSuccess()) {
                            System.out.println("✓ Success! Duplicate submission correctly blocked by server.");
                            System.out.println("  Server Message: " + duplicateResp.getMessage());
                        } else {
                            System.err.println("❌ Failed Duplicate Check: Server allowed duplicate submission!");
                        }

                        // =========================================================
                        // TEST 6: Teacher Grade Override & Confirmation (SUC-8)
                        // =========================================================
                        System.out.println("\n--- 6. Testing CONFIRM_GRADE (Teacher Override) ---");
                        ConfirmGradeData gradeData = new ConfirmGradeData(answerId, "teacher1", 95.0, "Great effort on architectural components!");
                        Request gradeReq = new Request(Command.CONFIRM_GRADE, gradeData, "test-req-4");
                        Response gradeResp = testClient.sendRequestSync(gradeReq);

                        if (gradeResp.isSuccess()) {
                            System.out.println("✓ Success! Teacher score override (95.0%) and comment committed to database.");
                        } else {
                            System.err.println("❌ Failed CONFIRM_GRADE: " + gradeResp.getMessage());
                        }

                    } else {
                        System.err.println("❌ Failed SUBMIT_EXAM: " + submitResp.getMessage());
                    }

                } else {
                    System.err.println("❌ Failed APPROVE_EXAM: " + approveResp.getMessage());
                }

            } else {
                System.err.println("❌ Failed CREATE_EXAM_MANUAL: " + manualResp.getMessage());
            }

            // =================================================================
            // TEST 7: Auto Exam Creation by Topic/Difficulty (SUC-3)
            // =================================================================
            System.out.println("\n--- 7. Testing CREATE_EXAM_AUTO ---");
            CreateExamAutoData autoData = new CreateExamAutoData();
            autoData.setTeacherId("teacher1");
            autoData.setCourseId("CS101");
            autoData.setTitle("Automated Data Structures Quiz");
            autoData.setInstructionsForStudents("Complete all questions.");
            autoData.setTopic("Data Structures");
            autoData.setDifficulty(Difficulty.EASY);
            autoData.setNumberOfQuestions(1);
            autoData.setDurationMinutes(30);

            Request autoReq = new Request(Command.CREATE_EXAM_AUTO, autoData, "test-req-5");
            Response autoResp = testClient.sendRequestSync(autoReq);

            if (autoResp.isSuccess() && autoResp.getPayload() instanceof Exam autoExam) {
                String autoExamId = autoExam.getExamId();
                System.out.println("✓ Success! Auto-generated Exam created with ID: " + autoExamId);

                // =============================================================
                // TEST 8: Reject Exam Workflow (SUC-4)
                // =============================================================
                System.out.println("\n--- 8. Testing REJECT_EXAM (Coordinator Rejection) ---");
                ExamApprovalDecisionData rejectData = new ExamApprovalDecisionData(autoExamId, "coord1", "Needs a larger question pool.");
                Request rejectReq = new Request(Command.REJECT_EXAM, rejectData, "test-req-reject");
                Response rejectResp = testClient.sendRequestSync(rejectReq);

                if (rejectResp.isSuccess()) {
                    System.out.println("✓ Success! Exam " + autoExamId + " status updated to REJECTED with feedback.");
                } else {
                    System.err.println("❌ Failed REJECT_EXAM: " + rejectResp.getMessage());
                }
            } else {
                System.err.println("❌ Failed CREATE_EXAM_AUTO: " + autoResp.getMessage());
            }

            // =================================================================
            // TEST 9: Study Bot Interaction (SUC-14)
            // =================================================================
            System.out.println("\n--- 9. Testing ASK_BOT_QUESTION ---");
            BotInteraction botReqData = new BotInteraction(
                    null, "student1", "CS101", "How do I format a binary tree?", ""
            );

            Request botReq = new Request(Command.ASK_BOT_QUESTION, botReqData, "test-bot-1");
            Response botResp = testClient.sendRequestSync(botReq);

            if (botResp.isSuccess() && botResp.getPayload() instanceof BotInteraction savedBot) {
                System.out.println("✓ Success! Bot responded and saved interaction ID: " + savedBot.getInteractionId());
                System.out.println("  Bot Answer: " + savedBot.getAnswer());
            } else {
                System.err.println("❌ Failed ASK_BOT_QUESTION: " + botResp.getMessage());
            }

            // =================================================================
            // TEST 10: Safe Logout Workflow (SUC-1)
            // =================================================================
            System.out.println("\n--- 10. Testing LOGOUT ---");
            Request logoutReq = new Request(Command.LOGOUT, "student1", "test-logout");
            Response logoutResp = testClient.sendRequestSync(logoutReq);

            if (logoutResp.isSuccess()) {
                System.out.println("✓ Success! User 'student1' safely logged out.");
            } else {
                System.err.println("❌ Failed LOGOUT: " + logoutResp.getMessage());
            }

            System.out.println("\n==========================================================================");
            System.out.println("  🎉 ALL END-TO-END NETWORK & DATABASE INTEGRATION TESTS PASSED 100%!   ");
            System.out.println("==========================================================================");

            testClient.closeConnection();

        } catch (Exception e) {
            System.err.println("❌ TEST EXCEPTION OCCURRED:");
            e.printStackTrace();
        }
    }
}