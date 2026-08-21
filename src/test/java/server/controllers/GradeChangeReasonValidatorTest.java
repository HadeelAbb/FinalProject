package server.controllers;

/**
 * Focused checks for CONFIRM_GRADE reason rules. No MySQL, OCSF, or Groq.
 * Official autoScore is the stored value passed into validate(); ConfirmGradeData
 * has no autoScore field, so a client cannot forge it through the DTO.
 */
public class GradeChangeReasonValidatorTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        check(GradeChangeReasonValidator.validate(80.0, 80.0, null) == null,
                "unchanged grade + null reason is allowed");
        check(GradeChangeReasonValidator.validate(80.0, 80.0, "") == null,
                "unchanged grade + empty reason is allowed");
        check(GradeChangeReasonValidator.validate(80.0, 80.0, "     ") == null,
                "unchanged grade + whitespace reason is allowed");

        check(GradeChangeReasonValidator.validate(80.0, 85.0, "Manual review correction") == null,
                "changed grade + valid reason is allowed");

        checkEquals(GradeChangeReasonValidator.REASON_REQUIRED,
                GradeChangeReasonValidator.validate(80.0, 85.0, null),
                "changed grade + null reason is denied");
        checkEquals(GradeChangeReasonValidator.REASON_REQUIRED,
                GradeChangeReasonValidator.validate(80.0, 85.0, ""),
                "changed grade + empty reason is denied");
        checkEquals(GradeChangeReasonValidator.REASON_REQUIRED,
                GradeChangeReasonValidator.validate(80.0, 85.0, "     "),
                "changed grade + whitespace reason is denied");

        checkEquals(GradeChangeReasonValidator.REASON_REQUIRED,
                GradeChangeReasonValidator.validate(70.0, 90.0, ""),
                "forged matching finalScore still denied against official stored autoScore 70");
        check(GradeChangeReasonValidator.validate(70.0, 70.0, "") == null,
                "confirming the official stored 70 without a reason is allowed");

        checkEquals(GradeChangeReasonValidator.FINAL_SCORE_REQUIRED,
                GradeChangeReasonValidator.validate(80.0, null, "reason"),
                "missing final score is rejected");

        Double storedWeighted = 40.0;
        check(GradeChangeReasonValidator.validate(storedWeighted, 40.0, null) == null,
                "weighted autoScore 40 confirmed unchanged without reason");
        check(GradeChangeReasonValidator.validate(storedWeighted, 45.0, "Manual correction") == null,
                "manual override of weighted 40 to 45 with reason is allowed");
        check(storedWeighted == 40.0,
                "validator does not mutate official autoScore");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  OK  - " + description);
        } else {
            System.out.println("FAIL  - " + description);
            failCount++;
        }
    }

    private static void checkEquals(String expected, String actual, String description) {
        check(expected.equals(actual), description + " (got: " + actual + ")");
    }
}
