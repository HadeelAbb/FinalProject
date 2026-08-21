package com.hsts.shared.model;

import java.util.Arrays;

/**
 * LAN-safe question illustration rules. Bytes travel with the serializable
 * Question; filenames are labels only. Never treat a path as a server file.
 */
public final class QuestionIllustration {

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final String TOO_LARGE = "Illustration is too large (max 2 MB).";
    public static final String NOT_IMAGE = "Illustration must be a PNG or JPG image.";

    private QuestionIllustration() {
    }

    public static boolean hasData(byte[] data) {
        return data != null && data.length > 0;
    }

    public static byte[] normalize(byte[] data) {
        return hasData(data) ? data : null;
    }

    public static byte[] copy(byte[] data) {
        byte[] normalized = normalize(data);
        return normalized == null ? null : Arrays.copyOf(normalized, normalized.length);
    }

    /**
     * @return an error message, or null if the illustration is absent or valid
     */
    public static String validate(byte[] data, String filename) {
        if (!hasData(data)) {
            return null;
        }
        if (data.length > MAX_BYTES) {
            return TOO_LARGE;
        }
        if (detectType(data) == null) {
            return NOT_IMAGE;
        }
        return null;
    }

    public static String detectType(byte[] data) {
        if (!hasData(data) || data.length < 4) {
            return null;
        }
        if (data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        return null;
    }

    /** Basename only — never a client filesystem path. */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String name = filename.replace('\\', '/').trim();
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1).trim();
        }
        if (name.isEmpty() || name.contains("..")) {
            return null;
        }
        if (name.length() > 255) {
            name = name.substring(0, 255);
        }
        return name;
    }

    public static String filenameForStorage(byte[] data, String filename) {
        if (!hasData(data)) {
            return null;
        }
        String name = sanitizeFilename(filename);
        return name != null ? name : "illustration";
    }

    public static void apply(Question question, byte[] data, String filename) {
        if (question == null) {
            return;
        }
        byte[] normalized = copy(data);
        question.setImageData(normalized);
        question.setImagePath(filenameForStorage(normalized, filename));
    }

    public static void copyOnto(Question source, Question target) {
        if (source == null || target == null) {
            return;
        }
        apply(target, source.getImageData(), source.getImagePath());
    }

    public static boolean sameBytes(byte[] left, byte[] right) {
        return Arrays.equals(normalize(left), normalize(right));
    }
}
