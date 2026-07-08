package com.hsts.shared.model;

import java.util.List;

/**
 * Client-side copy of Partner 1's SubjectCoordinator entity (PDOM).
 * A specialization of Teacher (per the requirements doc: a coordinator is
 * a teacher with the extra ability to approve/reject exams for her subject
 * before students can take them - SUC-4).
 */
public class SubjectCoordinator extends Teacher {

    private String subjectId;

    public SubjectCoordinator() {
    }

    public SubjectCoordinator(String id, String firstName, String lastName, String email,
                               List<Course> courses, String subjectId) {
        super(id, firstName, lastName, email, courses);
        setRole("SUBJECT_COORDINATOR");
        this.subjectId = subjectId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }
}
