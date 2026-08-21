package com.hsts.shared.model;

/**
 * Illustration filename/bytes rules. No MySQL, OCSF, or JavaFX.
 */
public class QuestionIllustrationTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        check(QuestionIllustration.validate(null, null) == null, "missing image is valid");
        check(QuestionIllustration.validate(new byte[0], "a.png") == null, "empty bytes are treated as no image");
        check(QuestionIllustration.validate(pngA(), "a.png") == null, "PNG is accepted");
        check(QuestionIllustration.validate(jpegB(), "b.jpg") == null, "JPEG is accepted");
        checkEquals(QuestionIllustration.NOT_IMAGE,
                QuestionIllustration.validate(new byte[]{1, 2, 3, 4, 5}, "a.png"),
                "non-image bytes are rejected");

        byte[] tooLarge = new byte[QuestionIllustration.MAX_BYTES + 1];
        tooLarge[0] = (byte) 0x89;
        tooLarge[1] = 0x50;
        tooLarge[2] = 0x4E;
        tooLarge[3] = 0x47;
        checkEquals(QuestionIllustration.TOO_LARGE,
                QuestionIllustration.validate(tooLarge, "a.png"),
                "oversized illustration is rejected");

        checkEquals("chart.png",
                QuestionIllustration.sanitizeFilename("C:\\Users\\Teacher\\Desktop\\chart.png"),
                "Windows path is reduced to a filename");
        checkEquals("chart.png",
                QuestionIllustration.sanitizeFilename("/home/teacher/chart.png"),
                "UNIX path is reduced to a filename");
        check(QuestionIllustration.filenameForStorage(null, "C:\\Users\\Teacher\\Desktop\\chart.png") == null,
                "filename is not stored without image bytes");
        checkEquals("chart.png",
                QuestionIllustration.filenameForStorage(pngA(), "C:\\Users\\Teacher\\Desktop\\chart.png"),
                "stored filename is the basename only");

        Question q = new Question("Q1", "text", "", Difficulty.EASY, "topic",
                "C:\\Users\\Teacher\\Desktop\\chart.png", "CS101", null);
        checkEquals("chart.png", q.getImagePath(), "Question stores basename, not a local path");
        q.setImageData(pngA());
        check(q.hasIllustration(), "bytes make hasIllustration true");
        check(QuestionIllustration.sameBytes(pngA(), q.getImageData()), "stored bytes match");

        Question copy = new Question();
        QuestionIllustration.copyOnto(q, copy);
        copy.getImageData()[0] = 0;
        check(q.getImageData()[0] == (byte) 0x89, "copyOnto does not alias the source byte array");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    public static byte[] pngA() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE,
                0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
                0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00,
                0x00, 0x03, 0x00, 0x01, 0x00, 0x05, (byte) 0xFE, (byte) 0xD4,
                (byte) 0xEF, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }

    public static byte[] jpegB() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9, 0x11, 0x22, 0x33};
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
        check(expected != null && expected.equals(actual), description + " (got: " + actual + ")");
    }
}
