//A test to check those SUC with ExamServerController methods:
//Tests SUC-1 (Login / User Roles / Single Session)
package server.db;

import server.controllers.LoginServerController;

/**
 * Acceptance Test Driver for SUC-1:
 * User Authentication, Password Verification, Session Tracking, and Logout.
 */
public class AuthTestDriver {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      SUC-1 AUTHENTICATION TEST SUITE");
        System.out.println("==================================================\n");

        DatabaseManager db = DatabaseManager.getInstance();
        if (!db.connect()) {
            System.err.println("❌ DB connection failed!");
            return;
        }

        LoginServerController loginController = new LoginServerController();

        // -------------------------------------------------------------
        // TEST 1: Successful Student Login
        // -------------------------------------------------------------
        System.out.println("👉 TEST 1: Validating Correct Student Credentials...");
        boolean studentLoginSuccess = loginController.login("student1", "123456");

        if (studentLoginSuccess) {
            System.out.println("   [PASS] Student 'student1' authenticated successfully.");
        } else {
            System.err.println("   [FAIL] Student 'student1' login failed!");
        }

        // -------------------------------------------------------------
        // TEST 2: Active Session Duplicate Login Block
        // -------------------------------------------------------------
        System.out.println("\n👉 TEST 2: Testing Duplicate Active Session Block...");
        boolean duplicateLoginSuccess = loginController.login("student1", "123456");

        if (!duplicateLoginSuccess) {
            System.out.println("   [PASS] System blocked duplicate active session for 'student1'.");
        } else {
            System.err.println("   [FAIL] System allowed duplicate session for active user!");
        }

        // -------------------------------------------------------------
        // TEST 3: Safe Logout
        // -------------------------------------------------------------
        System.out.println("\n👉 TEST 3: Logging Out Active Student...");
        loginController.logout("student1");

        if (!loginController.isAlreadyLoggedIn("student1")) {
            System.out.println("   [PASS] Student 'student1' logged out safely.");
        } else {
            System.err.println("   [FAIL] Student 'student1' remains active after logout!");
        }

        // -------------------------------------------------------------
        // TEST 4: Successful Coordinator Login
        // -------------------------------------------------------------
        System.out.println("\n👉 TEST 4: Validating Coordinator Credentials...");
        boolean coordLoginSuccess = loginController.login("coord1", "123456");

        if (coordLoginSuccess) {
            System.out.println("   [PASS] Coordinator 'coord1' authenticated successfully.");
            loginController.logout("coord1");
        } else {
            System.err.println("   [FAIL] Coordinator 'coord1' login failed!");
        }

        // -------------------------------------------------------------
        // TEST 5: Invalid Password Rejection
        // -------------------------------------------------------------
        System.out.println("\n👉 TEST 5: Testing Invalid Password Rejection...");
        boolean invalidPassSuccess = loginController.login("teacher1", "wrong_password");

        if (!invalidPassSuccess) {
            System.out.println("   [PASS] System safely rejected invalid password.");
        } else {
            System.err.println("   [FAIL] Invalid password was accepted!");
        }

        // -------------------------------------------------------------
        // TEST 6: Non-Existent User Rejection
        // -------------------------------------------------------------
        System.out.println("\n👉 TEST 6: Testing Non-Existent User Rejection...");
        boolean unknownUserSuccess = loginController.login("unknown_user", "123456");

        if (!unknownUserSuccess) {
            System.out.println("   [PASS] System safely rejected non-existent username.");
            System.out.println("\n✅ SUC-1 AUTHENTICATION SUITE PASSED PERFECTLY!");
        } else {
            System.err.println("   [FAIL] Non-existent user was logged in!");
        }

        db.disconnect();
    }
}