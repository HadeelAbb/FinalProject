package com.hsts.shared.model;

/**
 * Client-side copy of the School Principal entity (SUC 7.3).
 * The principal never creates or changes anything - only reads: all
 * questions, all exams (any status, any teacher), all results, and
 * statistical reports. Unlike SubjectCoordinator, this does NOT extend
 * Teacher - the spec is explicit that the principal doesn't teach.
 */
public class Principal extends User {

    public Principal() {
    }

    public Principal(String id, String firstName, String lastName, String email) {
        super(id, firstName, lastName, email, "PRINCIPAL");
    }
}