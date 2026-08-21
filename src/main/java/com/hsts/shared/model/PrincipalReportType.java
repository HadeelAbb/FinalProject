package com.hsts.shared.model;

/**
 * Comparison grouping for Principal statistical reports.
 * Additional similar types can be added without a new command stack.
 */
public enum PrincipalReportType {
    TEACHER("Teacher"),
    COURSE("Course"),
    STUDENT("Student");

    private final String label;

    PrincipalReportType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
